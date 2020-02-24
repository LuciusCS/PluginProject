package com.example.pluginproject.application;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.pluginproject.hook.HookActivity;
import com.example.pluginproject.hook.ProxyActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class HookApplicationTest extends Application {


    String TAG = HookActivity.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
//        hookAMS(this);

        try {
            hookAmsAction();

        } catch (Exception e) {
            Log.e(TAG, "Hook失败" + e.getMessage());
            e.printStackTrace();
        }

        try {
            hookLaunchActivity();

        } catch (Exception e) {
            Log.e(TAG, "Hook失败" + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 要在执行AMS之前，替换在AndroidManifest里面配置的Activity,
     */
    private void hookAmsAction() throws Exception {

        Method forName = Class.class.getDeclaredMethod("forName", String.class);
        Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);


        //动态代理

        Class mIActivityTaskManagerClass = (Class<?>) forName.invoke(null, "android.app.IActivityTaskManager");   //需要进行监听的接口

        //需要拿到ActivityManager对象，才能让动态代理里面的invoke正常执行下去
        //执行此方法的static public IActivityManager getDefault()  就能拿到IActivityManager
        Class mActivityTaskManagerClass2 = (Class<?>) forName.invoke(null, "android.app.ActivityTaskManager");

        Method method = (Method) getDeclaredMethod.invoke(mActivityTaskManagerClass2, "getService", null);

        final Object mActivityTaskManager = method.invoke(null);

        //本质是 IActivityManagerProxy
        Object mIActivityManagerProxy = Proxy.newProxyInstance(
                HookActivity.class.getClassLoader(),
                new Class[]{mIActivityTaskManagerClass},      //要监听的方法
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
                            //换成可以通过AMS检查的Activity ，用ProxyActivity可以绕过检查
                            //表示第二个参数
                            Intent intent = new Intent(HookApplicationTest.this, ProxyActivity.class);
                            intent.putExtra("actionIntent", (Intent) args[2]);

                            args[2] = intent;
                            Log.e(TAG,"使用代理类进行启动");
                        }

                        Log.e(TAG, "拦截到IActivityManager里面的方法++++++++++++++++++++++++=" + method.getName());

                        //让系统的继续正常往下执行
                        return method.invoke(mActivityTaskManager, args);
                    }
                });

        /**
         * 为了拿到gDefault
         * 通过ActivityManagerNative拿到gDefault变量（对象）
         */

        Class mActivityTaskManagerClass = Class.forName("android.app.ActivityTaskManager");
        Field iActivityTaskManagerSingletonField = mActivityTaskManagerClass.getDeclaredField("IActivityTaskManagerSingleton");
        iActivityTaskManagerSingletonField.setAccessible(true);   //授权
        Object iActivityTaskManagerSingleton = iActivityTaskManagerSingletonField.get(null);

        //替换点
        Class mSingletonClass = Class.forName("android.util.Singleton");

        //获取此字段mInstance
        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);   //让虚拟机不要检测 权限权限修饰符
        //替换
        mInstanceField.set(iActivityTaskManagerSingleton, mIActivityManagerProxy); //为了拿到gDefault 替换是需要gDefault
    }


    private void hookLaunchActivity() throws Exception {

        Field mCallbackField = Handler.class.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);  //授权

        /***
         *     //Handle对象的来源
         *     1、寻找 H  先寻找 ActivityThread
         *        执行此方法 public static ActivityThread currentActivityThread()
         *        通过ActivityThread找到 H
         *
         */

        Class mActivityThreadClass = Class.forName("android.app.ActivityThread");
        //获取ActivityThread对象
        Object mActivityThread = mActivityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field mHField = mActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        //获取真实对象
        Handler mh = (Handler) mHField.get(mActivityThread);


        mCallbackField.set(mh, new MyCallback(mh)); //替换，增加我们自己的实现代码


    }

    public static final int EXECUTE_TRANSACTION = 159;
    public static final int RELAUNCH_ACTIVITY = 160;

    class MyCallback implements Handler.Callback {



        private Handler mH;

        public MyCallback(Handler mH) {
            this.mH = mH;
        }


        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case  EXECUTE_TRANSACTION:
                //做我们自己额业务逻辑(把ProxyActivity换成TestActivity)

                Object obj = msg.obj;   //ActivityClientRecord

                //获取之前Hook携带的TestActivity
                try {
//                    Field intentField = obj.getClass().getDeclaredField("intent");
//                    intentField.setAccessible(true);
//                    // 获取intent对象，才能取出携带过来的actionIntent
//                    Intent intent = (Intent) intentField.get(obj);
//
//                    //actionIntent ==TestActivity的Intent
//                    Intent actionIntent = intent.getParcelableExtra("actionIntent");
//
//                    if (actionIntent!=null) {
//
//                        intentField.set(obj, actionIntent);
//                    }
                    //先把相关@hide的类都建好
                    Class<?> ClientTransactionClz = Class.forName("android.app.servertransaction.ClientTransaction");
                    Class<?> LaunchActivityItemClz = Class.forName("android.app.servertransaction.LaunchActivityItem");

                    Field mActivityCallbacksField = ClientTransactionClz.getDeclaredField("mActivityCallbacks");//ClientTransaction的成员
                    mActivityCallbacksField.setAccessible(true);
                    //类型判定，好习惯
                    if (!ClientTransactionClz.isInstance(msg.obj)) return true;
                    Object mActivityCallbacksObj = mActivityCallbacksField.get(msg.obj);//根据源码，在这个分支里面,msg.obj就是 ClientTransaction类型,所以，直接用
                    //拿到了ClientTransaction的List<ClientTransactionItem> mActivityCallbacks;
                    List list = (List) mActivityCallbacksObj;

                    if (list.size() == 0) return false;
                    Object LaunchActivityItemObj = list.get(0);//所以这里直接就拿到第一个就好了

                    if (!LaunchActivityItemClz.isInstance(LaunchActivityItemObj)) return true;
                    //这里必须判定 LaunchActivityItemClz，
                    // 因为 最初的ActivityResultItem传进去之后都被转化成了这LaunchActivityItemClz的实例

                    Field mIntentField = LaunchActivityItemClz.getDeclaredField("mIntent");
                    mIntentField.setAccessible(true);
                    Intent mIntent = (Intent) mIntentField.get(LaunchActivityItemObj);

                    Bundle extras = mIntent.getExtras();
                    if (extras != null) {
                        Intent oriIntent = (Intent) extras.get("actionIntent");
                        //那么现在有了最原始的intent，应该怎么处理呢？
                        mIntentField.set(LaunchActivityItemObj, oriIntent);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Hook失败" + e.getMessage());
                }
                break;

            }
            mH.handleMessage(msg);
            //让系统继续往下执行
//            return false;   //返回false系统会往下之执行
            return true;  //系统不会往下执行
        }
    }
    
}
