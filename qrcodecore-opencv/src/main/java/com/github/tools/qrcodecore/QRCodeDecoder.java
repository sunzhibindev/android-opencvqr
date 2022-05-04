package com.github.tools.qrcodecore;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.opencv.WeChatQRCodeDetector;

import java.util.List;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:16/4/8 下午11:22
 * 描述:解析二维码图片。一维条码、二维码各种类型简介 https://blog.csdn.net/xdg_blog/article/details/52932707
 */
public class QRCodeDecoder {

    private QRCodeDecoder() {
    }

    /**
     * 同步解析本地图片二维码。该方法是耗时操作，请在子线程中调用。
     *
     * @param picturePath 要解析的二维码图片本地路径
     * @return 返回二维码图片里的内容 或 null
     */
    public static String syncDecodeQRCode(String picturePath) {
        return syncDecodeQRCode(CvQrCodeUtil.getDecodeAbleBitmap(picturePath));
    }

    /**
     * 同步解析bitmap二维码。该方法是耗时操作，请在子线程中调用。
     *
     * @param bitmap 要解析的二维码图片
     * @return 返回二维码图片里的内容 或 null
     */
    public static String syncDecodeQRCode(Bitmap bitmap) {
        long l = SystemClock.elapsedRealtime();
        try {
            List<String> list = WeChatQRCodeDetector.detectAndDecode(bitmap);
            if (list.size() > 0) {
                return list.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("QRCodeDecoder", "syncDecodeQRCode: 耗时： " + (SystemClock.elapsedRealtime() - l));
        return "";
    }
}