# README

[APK Download](Calibration.apk)

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
