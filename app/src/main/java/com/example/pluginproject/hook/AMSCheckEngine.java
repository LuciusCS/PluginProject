package com.example.pluginproject.hook;

import android.content.Context;
import android.content.Intent;

import com.example.pluginproject.application.AndroidSDKVersion;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 *
 */
public class AMSCheckEngine {
    /**
     * 适用于 21以下版本 以及 21_22_23_24_25  26_27_28等版本
     * @param mContext
     */
    public static void mHookAMS(final Context mContext) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        //公共代码
        Object mIActivityManagerSingleton=null;
        Object mIActivityManager=null;

        if (AndroidSDKVersion.isAndroidOS_26_27_28()){

            //@3的获取， 系统的IActivityManager.aidl
            Class mActivityManagerClass=Class.forName("android.app.ActivityManager");
            mIActivityManager=mActivityManagerClass.getMethod("getService").invoke(null);

            //@1的获取
            Field mIActivityManagerSingletonField=mActivityManagerClass.getDeclaredField("IActivityManagerService");
            mIActivityManagerSingletonField.setAccessible(true);
            mIActivityManagerSingleton=mIActivityManagerSingletonField.get(null);
        }else if (AndroidSDKVersion.isAndroidOS_21_22_23_24_25()){

            //@3的获取
            Class mActivityManagerClass=Class.forName("android.app.ActivityManagerNative");
            Method getDefaultMethod=mActivityManagerClass.getDeclaredMethod("getDefault");
            getDefaultMethod.setAccessible(true);
            mIActivityManager=getDefaultMethod.invoke(null);

            //@1 的获取gDefault
            Field gDefaultField=mActivityManagerClass.getDeclaredField("gDefault");
            gDefaultField.setAccessible(true);
            mIActivityManagerSingleton=gDefaultField.get(null);

        }

        //@2 的获取 动态代理
        Class mIActivityManagerClass=Class.forName("android.app.IActivityManager");
        final  Object finalMIActivityManager=mIActivityManager;
        Object mIActivityManagerProxy= Proxy.newProxyInstance(mContext.getClassLoader(),
                new Class[]{mIActivityManagerClass}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if ("startActivity".equals(method.getName())){
                            //将启动的Activity替换成 ProxyActivity
                            Intent proxyIntent=new Intent(mContext,ProxyActivity.class);

                            //将待启动的信息携带过去
                            Intent target=(Intent)args[2];
                            proxyIntent.putExtra(Parameter.TARGET_INTENT,target);
                            args[2]=proxyIntent;
                        }

                        //@ 3
                        return method.invoke(finalMIActivityManager,args);
                    }
                });

        if (mIActivityManagerSingleton==null||mIActivityManagerProxy==null){
            throw new IllegalStateException("未适配该版本，请单独处理该系统");
        }

        Class mSingletonClass=Class.forName("android.util.Singleton");

        Field mInstanceField=mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        //将系统中的IActivityManager 换成动态代理
        ///@ 1 @2
        mInstanceField.set(mIActivityManagerSingleton,mIActivityManagerProxy);




    }
}
