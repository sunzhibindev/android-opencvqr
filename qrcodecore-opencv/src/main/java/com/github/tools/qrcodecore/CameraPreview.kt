package com.github.tools.qrcodecore

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.hardware.Camera
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.lang.Exception

class CameraPreview @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private var mCamera: Camera? = null
    private var mPreviewing = false
    private var mSurfaceCreated = false
    private var mIsTouchFocusing = false
    private var mOldDist = 1f
    private val mCameraConfigurationManager by lazy {
        CameraConfigurationManager(context)
    }
    private var mDelegate: Delegate? = null

    init {
        holder.addCallback(this)
    }

    fun setCamera(camera: Camera?) {
        mCamera = camera
        camera?.runCatching {
            mCameraConfigurationManager.initFromCameraParameters(mCamera!!)
            if (mPreviewing) {
                requestLayout()
            } else {
                showCameraPreview()
            }
        }
    }

    fun setDelegate(delegate: Delegate?) {
        mDelegate = delegate
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        mSurfaceCreated = true
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (surfaceHolder.surface == null) {
            return
        }
        stopCameraPreview()
        showCameraPreview()
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        mSurfaceCreated = false
        stopCameraPreview()
    }

    fun reactNativeShowCameraPreview() {
        if (holder == null || holder.surface == null) {
            return
        }
        stopCameraPreview()
        showCameraPreview()
    }

    private fun showCameraPreview() {
        mCamera?.runCatching {
            mPreviewing = true
            val surfaceHolder = holder
            surfaceHolder.setKeepScreenOn(true)
            setPreviewDisplay(surfaceHolder)
            mCameraConfigurationManager.setDesiredCameraParameters(this)
            startPreview()
            if (mDelegate != null) {
                mDelegate!!.onStartPreview()
            }
            startContinuousAutoFocus()
        }?.onFailure {
            it.printStackTrace()
        }
    }

    fun stopCameraPreview() {
        mCamera?.runCatching {
            mPreviewing = false
            cancelAutoFocus()
            setOneShotPreviewCallback(null)
            stopPreview()
        }?.onFailure {
            it.printStackTrace()
        }
    }

    fun openFlashlight() {
        mCamera?.runCatching {
            if (flashLightAvailable()) {
                mCameraConfigurationManager.openFlashlight(this)
            }
        }?.onFailure {
            it.printStackTrace()
        }
    }

    fun closeFlashlight() {
        mCamera?.runCatching {
            if (flashLightAvailable()) {
                mCameraConfigurationManager.closeFlashlight(this)
            }
        }?.onFailure {
            it.printStackTrace()
        }
    }

    private fun flashLightAvailable(): Boolean {
        return isPreviewing() && context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    fun onScanBoxRectChanged(scanRect: Rect?) {
        var scanRect = scanRect
        if (scanRect == null || scanRect.left <= 0 || scanRect.top <= 0) {
            return
        }
        var centerX = scanRect.centerX()
        var centerY = scanRect.centerY()
        var rectHalfWidth = scanRect.width() / 2
        var rectHalfHeight = scanRect.height() / 2
        CvQrCodeUtil.printRect("转换前", scanRect)
        if (CvQrCodeUtil.isPortrait(context)) {
            var temp = centerX
            centerX = centerY
            centerY = temp
            temp = rectHalfWidth
            rectHalfWidth = rectHalfHeight
            rectHalfHeight = temp
        }
        scanRect = Rect(centerX - rectHalfWidth, centerY - rectHalfHeight, centerX + rectHalfWidth, centerY + rectHalfHeight)
        CvQrCodeUtil.printRect("转换后", scanRect)
        CvQrCodeUtil.d("扫码框发生变化触发对焦测光")
        handleFocusMetering(scanRect.centerX().toFloat(), scanRect.centerY().toFloat(), scanRect.width(), scanRect.height())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isPreviewing()) {
            return super.onTouchEvent(event)
        }
        if (event.pointerCount == 1 && event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
            if (mIsTouchFocusing) {
                return true
            }
            mIsTouchFocusing = true
            CvQrCodeUtil.d("手指触摸触发对焦测光")
            var centerX = event.x
            var centerY = event.y
            if (CvQrCodeUtil.isPortrait(context)) {
                val temp = centerX
                centerX = centerY
                centerY = temp
            }
            val focusSize: Int = CvQrCodeUtil.dp2px(context, 120F)
            handleFocusMetering(centerX, centerY, focusSize, focusSize)
        }
        if (event.pointerCount == 2) {
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_POINTER_DOWN -> mOldDist = CvQrCodeUtil.calculateFingerSpacing(event)
                MotionEvent.ACTION_MOVE -> {
                    val newDist: Float = CvQrCodeUtil.calculateFingerSpacing(event)
                    if (newDist > mOldDist) {
                        mCamera?.run { handleZoom(true, this) }
                    } else if (newDist < mOldDist) {
                        mCamera?.run { handleZoom(false, this) }
                    }
                }
            }
        }
        return true
    }

    private fun handleZoom(isZoomIn: Boolean, camera: Camera) {
        try {
            val params = camera.parameters
            if (params.isZoomSupported) {
                var zoom = params.zoom
                if (isZoomIn && zoom < params.maxZoom) {
                    CvQrCodeUtil.d("放大")
                    zoom++
                } else if (!isZoomIn && zoom > 0) {
                    CvQrCodeUtil.d("缩小")
                    zoom--
                } else {
                    CvQrCodeUtil.d("既不放大也不缩小")
                }
                params.zoom = zoom
                camera.parameters = params
            } else {
                CvQrCodeUtil.d("不支持缩放")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleFocusMetering(
        originFocusCenterX: Float, originFocusCenterY: Float,
        originFocusWidth: Int, originFocusHeight: Int
    ) {
        mCamera?.runCatching {
            var isNeedUpdate = false
            val focusMeteringParameters = this.parameters
            val size = focusMeteringParameters.previewSize
            if (focusMeteringParameters.maxNumFocusAreas > 0) {
                CvQrCodeUtil.d("支持设置对焦区域")
                isNeedUpdate = true
                val focusRect: Rect = CvQrCodeUtil.calculateFocusMeteringArea(
                    1f,
                    originFocusCenterX, originFocusCenterY,
                    originFocusWidth, originFocusHeight,
                    size.width, size.height
                )
                CvQrCodeUtil.printRect("对焦区域", focusRect)
                focusMeteringParameters.focusAreas = listOf(Camera.Area(focusRect, 1000))
                focusMeteringParameters.focusMode = Camera.Parameters.FOCUS_MODE_MACRO
            } else {
                CvQrCodeUtil.d("不支持设置对焦区域")
            }
            if (focusMeteringParameters.maxNumMeteringAreas > 0) {
                CvQrCodeUtil.d("支持设置测光区域")
                isNeedUpdate = true
                val meteringRect: Rect = CvQrCodeUtil.calculateFocusMeteringArea(
                    1.5f,
                    originFocusCenterX, originFocusCenterY,
                    originFocusWidth, originFocusHeight,
                    size.width, size.height
                )
                CvQrCodeUtil.printRect("测光区域", meteringRect)
                focusMeteringParameters?.meteringAreas = listOf(Camera.Area(meteringRect, 1000))
            } else {
                CvQrCodeUtil.d("不支持设置测光区域")
            }
            if (isNeedUpdate) {
                mCamera?.cancelAutoFocus()
                mCamera?.parameters = focusMeteringParameters
                mCamera?.autoFocus { success, camera ->
                    if (success) {
                        CvQrCodeUtil.d("对焦测光成功")
                    } else {
                        CvQrCodeUtil.e("对焦测光失败")
                    }
                    startContinuousAutoFocus()
                }
            } else {
                mIsTouchFocusing = false
            }
        }?.onFailure { e ->
            e.printStackTrace()
            CvQrCodeUtil.e("对焦测光失败：" + e.message)
            startContinuousAutoFocus()
        }
    }

    /**
     * 连续对焦
     */
    private fun startContinuousAutoFocus() {
        mIsTouchFocusing = false
        if (mCamera == null) {
            return
        }
        mCamera?.runCatching {
            val parameters = parameters
            // 连续对焦
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            this.parameters = parameters
            // 要实现连续的自动对焦，这一句必须加上
            cancelAutoFocus()
        }?.onFailure {
            CvQrCodeUtil.e("连续对焦失败")
        }
    }

    fun isPreviewing(): Boolean {
        return mCamera != null && mPreviewing && mSurfaceCreated
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        var height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        if (mCameraConfigurationManager.cameraResolution != null) {
            val cameraResolution: Point = mCameraConfigurationManager.cameraResolution!!
            // 取出来的cameraResolution高宽值与屏幕的高宽顺序是相反的
            val cameraPreviewWidth = cameraResolution.x
            val cameraPreviewHeight = cameraResolution.y
            if (width * 1f / height < cameraPreviewWidth * 1f / cameraPreviewHeight) {
                val ratio = cameraPreviewHeight * 1f / cameraPreviewWidth
                width = (height / ratio + 0.5f).toInt()
            } else {
                val ratio = cameraPreviewWidth * 1f / cameraPreviewHeight
                height = (width / ratio + 0.5f).toInt()
            }
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }

    interface Delegate {
        fun onStartPreview()
    }
}