# README

[APK Download](Calibration.apk)

## Fix Error

在Android 5上需要对ar1021芯片进行校正，官方的apk运行报错。

读了程序发现，程序是给ar1020芯片的，可我们现在用的是ar1021芯片，所以加入以下判断之后（主要是因为Linux驱动生成的设备文件目录变了），程序能够正常运行：

```Java
  protected void onCreate(Bundle savedInstanceState) {
      ...
      sysfsPath="/sys/kernel/ar1020";        
      File file =new File(sysfsPath);      
      // if path is not exist that may use ar1021 chip
      if (!file .exists()  && !file .isDirectory())        
      {         
          sysfsPath="/sys/kernel/ar1021";        
      } 
      ...   
  }
```

**防止因为param1变量出现空指针异常，需要另外修正如下**

```Java
  protected void onCreate(Bundle savedInstanceState) {
      ...
      if (null != bundle)
      {
          param1 = bundle.getString("param1");
          if (param1 == null)
              param1 = "";
  
          Log.v("MCHP","param1 set to: "+param1);
      }
      ...
  }
```

## Support Default Calibration

* Add calibration values before exit in Calibration Apk
  ```
    @Override
    protected void onStop() {
        ...
        Log.v("MCHP","minX: "+Integer.toString(calibrationData.getMinX()));
        Log.v("MCHP","minY: "+Integer.toString(calibrationData.getMinY()));
        Log.v("MCHP","maxX: "+Integer.toString(calibrationData.getMaxX()));
        Log.v("MCHP","maxY: "+Integer.toString(calibrationData.getMaxY()));
        Log.v("MCHP","swapAxes: "+Integer.toString(calibrationData.getSwapAxes()));
        Log.v("MCHP","invertX: "+Integer.toString(calibrationData.getInvertX()));
        Log.v("MCHP","invertY: "+Integer.toString(calibrationData.getInvertY()));
        ...
    }
  ```
* Calibration values output in logcat, This is 7" panel.
  ```
    ...
    V/MCHP    ( 1793): minX: 426
    V/MCHP    ( 1793): minY: 584
    V/MCHP    ( 1793): maxX: 3701
    V/MCHP    ( 1793): maxY: 3249
    V/MCHP    ( 1793): swapAxes: 0
    V/MCHP    ( 1793): invertX: 0
    V/MCHP    ( 1793): invertY: 0
    ...
  ```
* add start script at i.MX6 android boot file: `device/fsl/sabresd_6dq/init.rc`
  ```
    ...
    chmod 0777 system/etc/ar102x_Calibration.sh
    ...
    service ar102x_Cali /system/etc/ar102x_Calibration.sh
        class late_start
        oneshot
    ...
  ```
* add shell code in `device/fsl/sabresd_6dq/ar102x_Calibration.sh`, will copy to `/system/etc/ar102x_Calibration.sh` when build Android source code.
  ```Shell
    #!/system/bin/sh

    ar1020_path="/sys/kernel/ar1020"
    ar1021_path="/sys/kernel/ar1021"
    
    if [ -d $ar1020_path ]; then
        ar102x_path=$ar1020_path
    elif [ -d $ar1021_path ]; then
        ar102x_path=$ar1021_path
    else 
        exit -1
    fi
    
    if [ -d $ar102x_path ]; then
        width=`busybox fbset -fb /dev/graphics/fb0 | grep geometry | busybox cut -d " " -f 2`
        height=`busybox fbset -fb /dev/graphics/fb0 | grep geometry | busybox cut -d " " -f 3`
    
        if [ $width == "800" -a $height == "480" ]; then
            echo 426 > $ar102x_path/minX 
            echo 84  > $ar102x_path/minY 
            echo 701 > $ar102x_path/maxX 
            echo 249 > $ar102x_path/maxY 
            echo 0   > $ar102x_path/swapAxes
            echo 0   > $ar102x_path/invertX
            echo 0   > $ar102x_path/invertY
        fi
    fi 
  ```
* Make auto copy when building Android source code: `device/fsl/imx6/sabresd_6dq.mk`
  ```
    ...
    PRODUCT_COPY_FILES += \
            device/fsl/sabresd_6dq/ar102x_Calibration.sh:system/etc/ar102x_Calibration.sh \
            device/fsl/sabresd_6dq/init.rc:root/init.freescale.rc \
    ...
  ```

