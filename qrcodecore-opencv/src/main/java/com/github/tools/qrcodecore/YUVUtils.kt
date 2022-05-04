package com.github.tools.qrcodecore

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.renderscript.*
import android.util.Log
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

object YUVUtils {
    private var rs: RenderScript? = null
    private var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB? = null

    fun nv21ToMat(nv21: ByteArray, width: Int, height: Int): Mat {
        val mat = Mat(height + height / 2, width, CvType.CV_8UC1)
        //从(0,0)开始放数据,直到data放完或者矩阵被填满(若是多通道,则把当前位置的通道全部填满，才继续下一个位置，data长度必须整除通道数).
        mat.put(0, 0, nv21)
        val rgbMat = Mat()
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_YUV2RGBA_NV21, 4)
        return rgbMat;
    }

    fun nv21ToMat(nv21: ByteArray, width: Int, height: Int, angle: Int, rect: Rect?): Mat {
        val left = rect?.left ?: 0
        val top = rect?.top ?: 0
        var clipW = rect?.width() ?: width
        var clipH = rect?.height() ?: height

        //初始化一个矩阵,没数据
        val mat = Mat(height + height / 2, width, CvType.CV_8UC1)
        //从(0,0)开始放数据,直到data放完或者矩阵被填满(若是多通道,则把当前位置的通道全部填满，才继续下一个位置，data长度必须整除通道数).
        mat.put(0, 0, nv21)

        val rgbMat = Mat()
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_YUV2RGBA_NV21, 4)
//        val mCacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        Utils.matToBitmap(rgbMat, mCacheBitmap)

        // 裁剪
        val dst = rect?.run {
            when (angle) {
                90 -> {
                    Mat(rgbMat, org.opencv.core.Rect(top, left, clipH, clipW))

                }
                180 -> {
                    Mat(rgbMat, org.opencv.core.Rect(left, top, clipW, clipH))

                }
                270 -> {
                    Mat(rgbMat, org.opencv.core.Rect(height - top, width - left, clipH, clipW))

                }
                else -> {
                    Mat(rgbMat, org.opencv.core.Rect(left, top, clipW, clipH))
                }

            }
        } ?: rgbMat;

        // 旋转
        when (angle) {
            90 -> {
                // 矩阵转置
                Core.transpose(dst, dst);
                //0: 沿X轴翻转； >0: 沿Y轴翻转； <0: 沿X轴和Y轴翻转
                // 翻转模式，flipCode == 0垂直翻转（沿X轴翻转），flipCode>0水平翻转（沿Y轴翻转），flipCode<0水平垂直翻转（先沿X轴翻转，再沿Y轴翻转，等价于旋转180°）
                Core.flip(dst, dst, 1);
                val temp = clipW
                clipW = clipH
                clipH = temp
            }
            180 -> {
                Core.flip(dst, dst, 0);
                Core.flip(dst, dst, 1);
            }
            270 -> {
                Core.transpose(dst, dst);
                Core.flip(dst, dst, 0);
                val temp = clipW
                clipW = clipH
                clipH = temp
            }
            else -> {

            }
        }
//        val mCacheBitmap2 = Bitmap.createBitmap(width2, height2, Bitmap.Config.ARGB_8888)
//        Utils.matToBitmap(rgbMat, mCacheBitmap2)
        return dst;
    }

    fun nv21ToBitmap(context: Context, nv21: ByteArray, width: Int, height: Int): Bitmap {
        val realtime = SystemClock.elapsedRealtime()
        if (rs == null) {
            rs = RenderScript.create(context.applicationContext)
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        }
        val yuvType: Type.Builder = Type.Builder(rs, Element.U8(rs)).setX(nv21.size)
        val ins = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
        val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
        val out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)

        ins.copyFrom(nv21)
        yuvToRgbIntrinsic?.setInput(ins)
        yuvToRgbIntrinsic?.forEach(out)
        val bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.copyTo(bmpout)
        Log.i("识别耗时", "NV21 转 bitmap 耗时: " + (SystemClock.elapsedRealtime() - realtime))
        return bmpout;
    }
}