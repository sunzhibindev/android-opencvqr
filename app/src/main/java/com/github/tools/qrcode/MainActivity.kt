package com.github.tools.qrcode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    fun scan() {
        startActivity(Intent(this, ScanActivity::class.java))
    }

    fun scan(v: View) {
        val permissions =
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        val denied = checkPermission(permissions)

        if (denied.isEmpty()) {
            startActivity(Intent(this, ScanActivity::class.java))
        } else {
            requestPermission(denied.toTypedArray())
        }
    }


    private fun checkPermission(checkList: Array<String>): List<String> {
        val list: MutableList<String> = mutableListOf()
        for (s in checkList) {
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat
                    .checkSelfPermission(this, s)
            ) {
                list.add(s)
            }
        }
        return list
    }

    private fun requestPermission(needRequestList: Array<String>) {
        ActivityCompat
            .requestPermissions(
                this, needRequestList,
                100
            )
    }

    // 检测并识别二维码

    fun delete(view: View) {
    }
}