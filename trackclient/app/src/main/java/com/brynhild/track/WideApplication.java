package com.brynhild.track;

import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.bugly.crashreport.CrashReport.*;
import android.app.Application;
import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 *
 * @author  XX
 */
public class WideApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();



        // 保存挂机log到文件。不是每次都有效....
        //  但是每次能保证上传...
        UserStrategy strategy = new UserStrategy(getApplicationContext());
        strategy.setCrashHandleCallback(new CrashReport.CrashHandleCallback() {


            @Override
            public synchronized Map<String, String> onCrashHandleStart(int crashType, String errorType,
                                                          String errorMessage, String errorStack) {
                LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
//                map.put("Key", "Value");
////                Log.d("XXXX", "onCrashHandleStart errorType" + errorType);
//                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
//                String strTime = df.format(new Date());
//                String strLogFile = String.format("/sdcard/nds/log/dumps/android_%s.log", strTime);
//
//                try
//                {
//
//                    FileWriter fileWriter = new FileWriter(strLogFile);
//
//                    fileWriter.write(strTime);
//                    fileWriter.write(errorType);
//                    fileWriter.write(errorMessage);
//                    fileWriter.write(errorStack);
//                    fileWriter.close();
//                }
//                catch (IOException e)
//                {
//                    //
//                    e.printStackTrace();
//                }

                return map;
            }

            @Override
            public synchronized byte[] onCrashHandleStart2GetExtraDatas(int crashType, String errorType,
                                                           String errorMessage, String errorStack) {
                try {
                    return "Extra data.".getBytes("UTF-8");
                } catch (Exception e) {
                    return null;
                }

                //return null;
            }

        });



       /* Step 2 Bugly SDK初始化
        * 参数1：上下文对象
        * 参数2：APPID，平台注册时得到,注意替换成你的appId
        * 参数3：是否开启调试模式，调试模式下会输出'CrashReport'tag的日志
        * 参数4：在userStrategy中设置save2FileOnCallback，初始化Bugly时，把userStrategy做为参数传进去
        */
        CrashReport.initCrashReport(getApplicationContext(), "900019278", false, strategy);

    }
}
