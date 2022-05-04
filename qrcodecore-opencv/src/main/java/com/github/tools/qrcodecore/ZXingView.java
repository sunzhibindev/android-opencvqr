package com.github.tools.qrcodecore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.WeChatQRCodeDetector;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * copy from  https://github.com/bingoogolapple/BGAQRCode-Android
 */
public class ZXingView extends QRCodeView {

    public ZXingView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ZXingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void setupReader() {
    }


    @Override
    protected ScanResult processBitmapData(Bitmap bitmap) {
        return new ScanResult(QRCodeDecoder.syncDecodeQRCode(bitmap));
    }

    @Override
    protected ScanResult processData(byte[] data, int width, int height, boolean isRetry) {
        String result = null;
        Log.d("识别耗时", "图片处理 开始: ");
        long realtime = SystemClock.elapsedRealtime();
        List<Mat> pointList = new ArrayList<>();
        Rect scanBoxAreaRect = mScanBoxView.getScanBoxAreaRect();
        Mat matPic = YUVUtils.INSTANCE.nv21ToMat(data, width, height, 90, scanBoxAreaRect);
        Log.d("识别耗时", "图片处理 结束: " + (SystemClock.elapsedRealtime() - realtime));
        realtime = SystemClock.elapsedRealtime();
        List<String> list = WeChatQRCodeDetector.detectAndDecode(matPic, pointList);
        Log.d("识别耗时", "二维码耗时: " + (SystemClock.elapsedRealtime() - realtime));
        if (list.size() > 0) {
            result = list.get(0);
        } else {
            return new ScanResult(null);
        }
        if (pointList.size() <= 0) {
            return new ScanResult(result);
        }
        Mat mat = pointList.get(0);
        // 处理自动缩放和定位点
        boolean isNeedAutoZoom = isNeedAutoZoom();
        PointF[] pointArr = new PointF[mat.rows()];
        int pointIndex = 0;
        if (isShowLocationPoint() || isNeedAutoZoom) {
            for (int i = 0; i < mat.height(); i++) {
                float x = (float) mat.get(i, 0)[0];
                float y = (float) mat.get(i, 1)[0];
                pointArr[pointIndex] = new PointF(x, y);
                pointIndex++;
            }
            if (transformToViewCoordinates(pointArr, scanBoxAreaRect, isNeedAutoZoom, result)) {
                return null;
            }
        }
        Log.e("识别耗时", "processData: " + (SystemClock.elapsedRealtime() - realtime));
        return new ScanResult(result);
    }

    private boolean isNeedAutoZoom() {
        return isAutoZoom();
    }
}