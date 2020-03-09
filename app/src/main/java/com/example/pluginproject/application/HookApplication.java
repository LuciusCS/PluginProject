package com.example.pluginproject.application;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.example.pluginproject.hook.ProxyActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.PropertyResourceBundle;


public class HookApplication extends Application {


    String TAG = HookApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            hookAmsAction();

        } catch (Exception e) {
            Log.e(TAG, "Hook失败" + e.getMessage());
            e.printStackTrace();
        }

        try {
            hookLaunchActivity();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"Hook失败"+e.getMessage());
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
                HookApplication.class.getClassLoader(),
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
                            //换成可以通过AMS检查的Activity

                            Intent intent = new Intent(HookApplication.this, ProxyActivity.class);
                            intent.putExtra("actionIntent", ((Intent) args[2])); //把之前的TestActivity携带过去，后面进行还原
                            args[2] = intent;
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


    /**
     * Hook LaunchActivity
     *
     * @throws Exception
     */
    private void hookLaunchActivity() throws Exception {
        Field mCallbackField = Handler.class.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);

        /**
         * handler对象的生成，需要查找 h
         *
         * 先寻找ActivityThread
         *
         * 执行 ActivityThread中的
         * public static ActivityThread currentActivityThread() {
         *         return sCurrentActivityThread;
         *     }
         *
         * 通过 ActivityThread 查找到 H
         */

        Class mActivityClassThreadClass = Class.forName("android.app.ActivityThread");
        //获得ActivityThread 对象
        Object mActivityThread = mActivityClassThreadClass.getMethod("currentActivityThread").invoke(null);//因为currentActivityThread方法为静态方法，所以可以传null


        Field mHField = mActivityClassThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        //获取真正的对象
        Handler mH=(Handler)mHField.get(mActivityThread);

        mCallbackField.set(mH, new MyCallback(mH));  //替换  增加我们自己的实现代码，先执行完自定义的callback，再执行系统的callback中的handlerMessage()

    }

    public static final int LAUNCH_ACTIVITY = 100;

    class MyCallback implements Handler.Callback {

        private Handler mH;

        public MyCallback(Handler mH) {
            this.mH = mH;
        }

        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case LAUNCH_ACTIVITY:
                //做我们自己的业务逻辑（把ProxyActivity换成我们自己的Activity）
//                mH.handleMessage(msg);

                Object obj = msg.obj;
                //获取之前Hook携带过来的TestActivity
                try {
                    //这里的intent是ActivityThr
                    Field intentField = obj.getClass().getDeclaredField("intent");
                    intentField.setAccessible(true);

                    //获取intent的对象，才能取出携带过来的表示
                    Intent intent = (Intent) intentField.get(obj);
                    //actionIntent ==TestActivity的Intent
                    Intent actionIntent = intent.getParcelableExtra("actionIntent");

                    if (actionIntent != null) {
                        intentField.set(obj, actionIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "MyCallback" + e.getMessage());
                }
                break;
            }
            mH.handleMessage(msg);

            //让系统继续往下执行
//            return false; //返回false, 系统就会往下执行
            return true; //返回true, 系统就不会往下执行
            /**
             * 查看Handler 中的 dispatchMessage(Message msg)  方法，看return
             */
        }
    }


}
