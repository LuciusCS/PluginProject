package com.example.pluginproject.application;

import android.app.Application;
import android.content.res.Resources;

import com.example.pluginproject.hook.AMSCheckEngine;
import com.example.pluginproject.hook.ActivityThreadmHRestore;
import com.example.pluginproject.hook.DexElementFuse;
import com.example.pluginproject.hook.Parameter;

import java.lang.reflect.InvocationTargetException;

//import java.lang.reflect.Parameter;

public class MyApplication extends Application {

    //01:54:43

    public static  String pluginPath;

    DexElementFuse dexElementFuse;

    @Override
    public void onCreate() {
        super.onCreate();

        //获取插件的路径
        pluginPath=ApkCopyAssetsToDir.copyAssetToCache(this, Parameter.PLUGIN_FILE_NAME);

        //第一个 Hook 绕过AMS的检查
        try {
            AMSCheckEngine.mHookAMS(this);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        //第二个Hook
        try {
            ActivityThreadmHRestore.mActivityThreadmHAction(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //进行融合
        dexElementFuse=new DexElementFuse();
        try {
            dexElementFuse.mainPluginFuse(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 重写资源管理器为了 提供资源文件的加载工作
     * 虽然每一个Activity 都有资源管理器，但不可能为每一个Activity重写资源管理器，只需要在Application中重写即可
     * @return
     */
    @Override
    public Resources getResources() {
        return dexElementFuse.getResources()==null?super.getResources():dexElementFuse.getResources();
    }
}
