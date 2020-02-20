package com.example.plugin_package;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.stander.ServiceInterface;

public class TestService extends BaseService {

    static final String TAG = TestService.class.getSimpleName();


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        //开启子线程，执行耗时任务
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {

                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        Log.e("+++++++++++", "插件服务正在执行中");
                    }
                }
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
