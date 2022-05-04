package com.github.tools.qrcodecore;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.Log;

import com.github.tools.qrcodecore.utils.ThreadUtils;

import java.lang.ref.WeakReference;

class ProcessDataTask extends ThreadUtils.Task<ScanResult> {
    private Camera mCamera;
    private byte[] mData;
    private boolean mIsPortrait;
    private String mPicturePath;
    private Bitmap mBitmap;
    private WeakReference<QRCodeView> mQRCodeViewRef;
    private static long sLastStartTime = 0;

    private static final String TAG = "ProcessDataTask";

    ProcessDataTask(Camera camera, byte[] data, QRCodeView qrCodeView, boolean isPortrait) {
        mCamera = camera;
        mData = data;
        mQRCodeViewRef = new WeakReference<>(qrCodeView);
        mIsPortrait = isPortrait;
    }

    ProcessDataTask(String picturePath, QRCodeView qrCodeView) {
        mPicturePath = picturePath;
        mQRCodeViewRef = new WeakReference<>(qrCodeView);
    }

    ProcessDataTask(Bitmap bitmap, QRCodeView qrCodeView) {
        mBitmap = bitmap;
        mQRCodeViewRef = new WeakReference<>(qrCodeView);
    }

    @Override
    public ScanResult doInBackground() {
        Log.d(TAG, "doInBackground: ");
        return doInBackgroundInner();
    }

    @Override
    public void onSuccess(ScanResult result) {
        Log.d(TAG, "onSuccess: ");
        onPostExecute(result);
    }

    @Override
    public void onCancel() {
        Log.d(TAG, "onCancel: ");
        onCancelled();
    }

    @Override
    public void onFail(Throwable t) {
        Log.d(TAG, "onFail: ");
        onCancelled();
    }


    protected void onCancelled() {
        mQRCodeViewRef.clear();
        mBitmap = null;
        mData = null;
    }

    private ScanResult processData(QRCodeView qrCodeView) {
        if (mData == null) {
            return null;
        }

        int width = 0;
        int height = 0;
        byte[] data = mData;
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            width = size.width;
            height = size.height;

            if (mIsPortrait) {
                // 竖屏需要顺时针旋转90度
                // data = new byte[mData.length];
                // for (int y = 0; y < height; y++) {
                //     for (int x = 0; x < width; x++) {
                //         data[x * height + height - y - 1] = mData[x + y * width];
                //     }
                // }
                // int tmp = width;
                // width = height;
                // height = tmp;
            }

            return qrCodeView.processData(data, width, height, false);
        } catch (Exception e1) {
            e1.printStackTrace();
            try {
                if (width != 0 && height != 0) {
                    CvQrCodeUtil.d("识别失败重试");
                    return qrCodeView.processData(data, width, height, true);
                } else {
                    return null;
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                return null;
            }
        }
    }

    private ScanResult doInBackgroundInner() {
        QRCodeView qrCodeView = mQRCodeViewRef.get();
        if (qrCodeView == null) {
            return null;
        }

        if (mPicturePath != null) {
            return qrCodeView.processBitmapData(CvQrCodeUtil.getDecodeAbleBitmap(mPicturePath));
        } else if (mBitmap != null) {
            ScanResult result = qrCodeView.processBitmapData(mBitmap);
            mBitmap = null;
            return result;
        } else {
            if (CvQrCodeUtil.isDebug()) {
                CvQrCodeUtil.d("两次任务执行的时间间隔：" + (System.currentTimeMillis() - sLastStartTime));
                sLastStartTime = System.currentTimeMillis();
            }
            long startTime = System.currentTimeMillis();

            ScanResult scanResult = processData(qrCodeView);

            if (CvQrCodeUtil.isDebug()) {
                long time = System.currentTimeMillis() - startTime;
                if (scanResult != null && !TextUtils.isEmpty(scanResult.result)) {
                    CvQrCodeUtil.d("识别成功时间为：" + time);
                } else {
                    CvQrCodeUtil.e("识别失败时间为：" + time);
                }
            }

            return scanResult;
        }
    }

    protected void onPostExecute(ScanResult result) {
        QRCodeView qrCodeView = mQRCodeViewRef.get();
        if (qrCodeView == null) {
            return;
        }

        if (mPicturePath != null || mBitmap != null) {
            mBitmap = null;
            qrCodeView.onPostParseBitmapOrPicture(result);
        } else {
            qrCodeView.onPostParseData(result);
        }
    }
}
