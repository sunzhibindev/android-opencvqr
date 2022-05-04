package com.github.tools.qrcode

import android.app.Application
import org.opencv.WeChatQRCodeDetector


class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //初始化WeChatQRCodeDetector
        WeChatQRCodeDetector.init(this)

    }
}