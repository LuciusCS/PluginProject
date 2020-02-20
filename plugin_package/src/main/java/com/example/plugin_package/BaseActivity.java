package com.example.plugin_package;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
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
        intentNew.putExtra("className",intent.getComponent().getClassName());

        appActivity.startActivity(intentNew);
    }
}
