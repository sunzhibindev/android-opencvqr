package com.github.tools.qrcode

import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.tools.qrcode.R
import com.github.tools.qrcodecore.QRCodeView
import com.github.tools.qrcodecore.ZXingView

class ScanActivity : AppCompatActivity(), QRCodeView.Delegate {
    private lateinit var zXingView: ZXingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        zXingView = findViewById<ZXingView>(R.id.zXingView)

        zXingView.setDelegate(this)
        zXingView.showScanRectAndStopMoveLine()
        zXingView.startSpotAndShowRect()

        zXingView.startCamera()

    }

    override fun onScanQRCodeSuccess(result: String?) {
        Toast.makeText(this, result, Toast.LENGTH_LONG).show()
        zXingView.postDelayed({
            zXingView.startSpotAndShowRect()
        }, 3000)
    }

    override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {

    }

    override fun onScanQRCodeOpenCameraError() {
        Toast.makeText(this, "扫描错误", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        findViewById<ZXingView>(R.id.zXingView).stopCamera()
    }


}