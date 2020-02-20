package com.example.plugin_package;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import com.example.stander.ActivityInterface;

public class BaseActivity extends Activity implements ActivityInterface {


    public Activity appActivity; //宿主的环境

    @Override
    public void insertAppContext(Activity appActivity) {
        this.appActivity=appActivity;
    }

    /**
     * 使用 @SuppressLint 的目的是不想调用父类
     * @param savedInstanceState
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void onCreate(Bundle savedInstanceState) {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onStart() {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onResume() {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onDestroy() {

    }


    public void setContentView(int resId) {
        appActivity.setContentView(resId);
    }

    public View findViewById(int layoutId){
        return appActivity.findViewById(layoutId);
    }


    public void startActivity(Intent intent) {

        Intent intentNew=new Intent();
        intentNew.putExtra("className",intent.getComponent().getClassName());   //TestActivity全类名

        appActivity.startActivity(intentNew);
    }

    @Override
    public ComponentName startService(Intent service) {
        Intent intentNew=new Intent();
        intentNew.putExtra("className",service.getComponent().getClassName());  //TestService全类名

        return appActivity.startService(intentNew);
    }

    //注册广播，使用宿主环境
    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return appActivity.registerReceiver(receiver, filter);
    }

    //发送广播

    @Override
    public void sendBroadcast(Intent intent) {
       appActivity.sendBroadcast(intent);
    }
}
