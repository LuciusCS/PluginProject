package com.example.pluginproject.application;

import android.app.Application;
import android.util.Log;

import com.example.pluginproject.hook.HookActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class HookApplication extends Application {


    String TAG = HookActivity.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            hookAmsAction();

        } catch (Exception e) {
            Log.e(TAG, "Hook失败" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 要在执行AMS之前，替换在AndroidManifest里面配置的Activity
     */
    private void hookAmsAction() throws Exception {



        //动态代理

            Class mIActivityManagerClass = Class.forName("android.app.IActivityManager");   //需要进行监听的接口

        //需要拿到ActivityManager对象，才能让动态代理里面的invoke正常执行下去
        //执行此方法的static public IActivityManager getDefault()  就能拿到IActivityManager
           Class mActivityManagerNativeClass2 = Class.forName("android.app.ActivityManagerNative");

            final Object mIActivityManager = mActivityManagerNativeClass2.getMethod("getDefault").invoke(null);

        //本质是 IActivityManagerProxy
        Object mIActivityManagerProxy = Proxy.newProxyInstance(
                HookActivity.class.getClassLoader(),
                new Class[]{mIActivityManagerClass},      //要监听的方法
                new InvocationHandler() {   //接口的回调方法

                    /**
                     *
                     * @param proxy
                     * @param method  IActivityManager 里面的方法
                     * @param args    IActivityManager里面的参数
                     * @return
                     * @throws Throwable
                     */
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if ("startActivity".equals(method.getName())) {
                            // 做自己的业务逻辑
                        }

                        Log.e(TAG, "拦截到IActivityManager里面的方法" + method.getName());

                        //让系统的继续正常往下执行
                        return method.invoke(mIActivityManager, args);
                    }
                });

        /**
         * 为了拿到gDefault
         * 通过ActivityManagerNative拿到gDefault变量（对象）
         */

        Class mActivityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
        Field gDefaultField = mActivityManagerNativeClass.getDeclaredField("gDefault");
        gDefaultField.setAccessible(true);   //授权
        Object gDefault = gDefaultField.get(null);

        //替换点
        Class mSingletonClass = Class.forName("android.util.Singleton");

        //获取此字段mInstance
        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);   //让虚拟机不要检测 权限权限修饰符
        //替换
        mInstanceField.set(gDefault, mIActivityManagerProxy); //为了拿到gDefault 替换是需要gDefault
    }
}
