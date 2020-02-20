package com.example.plugin_package;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.stander.ServiceInterface;

public class BaseService extends Service implements ServiceInterface {


    public Service appService; //宿主的环境



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void insertAppContext(Service appService) {
        this.appService=appService;
    }

    @Override
    public void onCreate() {

    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 0;
    }

    @Override
    public void onDestroy() {

    }
}
