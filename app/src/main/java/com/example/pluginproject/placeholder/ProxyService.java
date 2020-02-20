package com.example.pluginproject.placeholder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.stander.ServiceInterface;

public class ProxyService  extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String className=intent.getStringExtra("className");

        try {
            Class mTestServiceClass = PluginManager.getInstance(this).getClassLoader().loadClass(className);
            Object mTestService=mTestServiceClass.newInstance();

            ServiceInterface serviceInterface=(ServiceInterface)mTestService;

            //注入组件环境
            serviceInterface.insertAppContext(this);

            serviceInterface.onStartCommand(intent,flags,startId);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
