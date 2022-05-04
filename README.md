# 编译opencv
参考自：https://blog.csdn.net/wang382758656/article/details/114762661

opencv 模块介绍：http://qt.digitser.net/opencv/4.2.0/zh-CN/index.html
基于opencv 4.5.5版本编译，使用WeChat QrCode 模块提升二维码识别速度。 较直接用zxing识别速度慢，识别率高。提升部分老手机识别率
## 准备工作:

### cmake、 jdk 、ant 、opencv-4.5.5和opencv-contrib 、ndk-22.0.7026061

## 编译步骤: 打开cmake-gui

1. **Add Entry**

   - ANDROID_NDK 类型为PATH，填入你的NDK-22路径
   - ANDROID_SDK    PATH，填入你的SDK路径
   - ANDROID_ABI STRING，设置平台，不填默认为 armeabi-v7a
   - ANDROID_NATIVE_API_LEVEL STRING，默认API为21
   - ANT_EXECUTABLE PATH，填入ANT路径下的bin（用于java 封装，便于 AS 导入module）
   - ANDROID_STL STRING，根据需求写入c++_static或c++_shared（本次操作填入c++_shared）

2. **配置toolchain**

   点击Configure,选择Specify toolchain file for cross-compiling，点击Continue，选择对应NDK目录下的toolchain路径，点击Done

3. **添加opencv_contrib模块并调整参数**

   OPENCV_EXTRA_MODULES_PATH，选择opencv_contrib/modules路径

4. **执行Configure和Generate**

5. build 目录执行make -j6

6. 成功后, 打开opencv-android

   > 默认编译出来26M, 比官方的大, 按照官方的目录结构适当裁剪不需要的模块
   >
   > 例如:face text ml 结构光等等用不到的模块, 其他裁剪待发现, 建议一个参数一个参数裁剪,避免过多报错
   >
   > 
   >
   > 建议自行按需编译, 尽量减小so. 默认提供的不是最精简的so

# 基于OpenCV开源的微信二维码引擎

```.java
源码中: 模块 qrcodecore-library 扫码功能模块
       模块 opencv 源码编译出来 
```

扫码模块用的camera, camera2以后安排

感谢：https://github.com/jenly1314/WeChatQRCode
感谢：https://github.com/bingoogolapple/BGAQRCode-Android