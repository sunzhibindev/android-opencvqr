package org.opencv;

import android.content.Context;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

/**
 * @author sunzhibindev@gmail.com
 */
public class OpencvInit {
    private static final String TAG = "OpencvInit";

    private OpencvInit() {
        throw new AssertionError();
    }

    /**
     * 异步初始化
     *
     * @param AppContext
     * @param loaderCallback
     */
    public static void initAsync(Context AppContext, LoaderCallbackInterface loaderCallback) {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, AppContext, loaderCallback);
    }

    /**
     * 同步初始化
     *
     * @return
     */
    public static boolean initSync() {
        return OpenCVLoader.initDebug();
    }


    public static void initAsync(Context context) {
        LoaderCallbackInterface loaderCallback = new BaseLoaderCallback(context) {
            @Override
            public void onManagerConnected(int status) {
                Log.i(TAG, "onManagerConnected:" + status);
                if (status == SUCCESS) {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
            }
        };
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, loaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

}
