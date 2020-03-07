package com.example.pluginproject.application;

import android.app.Application;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.pluginproject.hook.HookActivity;
import com.example.pluginproject.hook.ProxyActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.logging.Handler;

public class HookApplication extends Application {


    String TAG = HookActivity.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        // TODO Hook 1 绕过AMS检查
        try {
            hookAmsAction();

        } catch (Exception e) {
            Log.e(TAG, "Hook失败" + e.getMessage());
            e.printStackTrace();
        }

        // TODO Hook 2 还原ProxyActivity

        try {
            hookActivityThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 要在执行AMS之前，替换在AndroidManifest里面配置的Activity
     */
    private void hookAmsAction() throws Exception {
        /**
         * @1 怎么来（反射）， @1是 IActivityManagerSingleton //查看startActivity的源码
         */
        Class mActivityManagerClass = Class.forName("android.app.ActivityManager");
        Field mActivityManagerSingletonField = mActivityManagerClass.getDeclaredField("IActivityManagerSingleton");
        mActivityManagerSingletonField.setAccessible(true);   //授权
        Object IActivityManagerSingleton = mActivityManagerSingletonField.get(null);

        /**
         * 为了拿到ActivityTaskManager
         * 通过IActivityManagerSingleton拿到IActivityManagerSingleton
         */
        //替换点
        Class mSingletonClass = Class.forName("android.util.Singleton");

        //获取此字段mInstance
        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);   //让虚拟机不要检测 权限权限修饰符


        /**
         * @3 android.app.IActivityManager.aidl 9.0
         */
        //动态代理
        Class mIActivityManagerClass = Class.forName("android.app.IActivityManager");   //需要进行监听的接口

        /**
         * @4 真实系统的IActivityManage
         */
        //需要拿到ActivityManager对象，才能让动态代理里面的invoke正常执行下去
        //执行此方法的static public IActivityManager getDefault()  就能拿到IActivityManager
        final Object mIActivityManager = mIActivityManagerClass.getMethod("getService").invoke(null);

        /**
         * @2 要检测启动 startActivity的行为，所以需要使用动态代理
         */
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
                            //下标2 == Intent 更换代理Activity
                            Intent proxyIntent = new Intent(HookApplication.this, ProxyActivity.class);
                            //考虑到后面的还原，必须携带目标过去
                            proxyIntent.putExtra("targetIntent", (Intent) args[2]);
                            args[2] = proxyIntent;

                        }

                        Log.e(TAG, "拦截到IActivityManager里面的方法" + method.getName());

                        /**
                         * @4 method.invoke() 第一个参数
                         */
                        //让系统的继续正常往下执行
                        return method.invoke(mIActivityManager, args);
                    }
                });


        /**
         * mInstanceField.set() 第二个参数，
         * 要检测startActivity的行为使用动态代理
         */
        //替换
        mInstanceField.set(IActivityManagerSingleton, mIActivityManagerProxy); //为了拿到gDefault 替换是需要gDefault
    }

    //Android对public方法修改较少，主要修改私有的方法
    private void hookActivityThread() throws Exception {


        /**
         * @1 是 mH
         */

        Class mActivityThreadClass = Class.forName("android.app.ActivityThread");
        Field mhField = mActivityThreadClass.getDeclaredField("mh");
        mhField.setAccessible(true);

        /**
         * @3 ActivityThread
         */
        Object mActivityThread = mActivityThreadClass.getMethod("currentActivityThread").invoke(null);

        Object mh = mhField.get(mActivityThread);

        //倒序 【第一步】 ，将 mCallback 换成自己的
        Field mCallbackField = Handler.class.getDeclaredField("mCallback");

        mCallbackField.setAccessible(true);
        mCallbackField.set(mh, new MyCallback());

    }

    //Callback mCallback使用自己定义的callback
    private class MyCallback implements android.os.Handler.Callback {

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            //需要分析源码的写法
            /**
             *
             * 拿到 Intent(ProxyActivity)
             *
             * case EXECUTE_TRANSACTION:
             *  final ClientTransaction transaction = (ClientTransaction) msg.obj;
             *  mTransactionExecutor.execute(transaction);
             *  if (isSystem()) {
             *     // Client transactions inside system process are recycled on the client side
             *     // instead of ClientLifecycleManager to avoid being cleared before this
             *    // message is handled.
             *    transaction.recycle();
             *  }
             *  // TODO(lifecycler): Recycle locally scheduled transactions.
             *   break;
             *
             *  从 final ClientTransaction transaction = (ClientTransaction) msg.obj开始
             *  集 msg.obj
             *
             *   ClientTransaction 有 private List<ClientTransactionItem> mActivityCallbacks;
             *
             *   ClientTransactionItem 有一个子类 LaunchActivityItem
             *
             *   LaunchActivityItem 其中有mIntent
             *
             *   LaunchActivityItem 继承自 ClientTransactionItem， LaunchActivityItem会向上转型 为ClientTransactionItem
             *
             *
             *
             *  如果没有msg.obj 则 Hook 失败
             *
             *   ClientTransactionItem 向上强制转化为   ClientTransaction 即  final ClientTransaction transaction = (ClientTransaction) msg.obj;
             *
             */

            //开始源
            Object mClientTransaction = msg.obj;


            /**
             * //第一步换掉Intent
             */

            try {
                /**
                 * @1 mIntentField.set())的第一个参数  LaunchActivityItem
                 *    private List<ClientTransactionItem> mActivityCallbacks;
                 */

                //下面一行会执行多次，才会将Activity 显示出来
                Class mLaunchActivityItemClass = Class.forName("android.app.servertransaction.LaunchActivityItem");


                Field mActivityCallbacksField = mClientTransaction.getClass().getDeclaredField("mActivityCallbacks");
                mActivityCallbacksField.setAccessible(true);
                List mActivityCallbacks = (List) mActivityCallbacksField.get(mClientTransaction);

                if (mActivityCallbacks.size() == 0) {
                    return false;
                }

                //Activity 的启动一定是 第 0个
                Object mLaunchActivityItem = mActivityCallbacks.get(0);
                //为什么要取出集合的 item 和 mLaunchActivityItemClass 类型做对比
                //callback中可能会有ActivityResultItem，从源码中可以观察到
                //ActivityResultItem extends ClientTransactionItem
                if (mLaunchActivityItemClass.isInstance(mLaunchActivityItem) == false) {
                    return false;
                }


                Field mIntentField = mLaunchActivityItemClass.getDeclaredField("mIntent");
                mIntentField.setAccessible(true);

                /**
                 * @ 2  mIntentField.set() 的第二个参数  LaunchActivityItem Intent mIntent
                 *
                 * 拿到intent才能拿到ProxyActivity，然后拿到TestActivity
                 *
                 */

                Intent proxyIntent = (Intent) mIntentField.get(mLaunchActivityItem);

                //目标的intent
                Intent targetIntent = proxyIntent.getParcelableExtra("targetIntent");
                if (targetIntent != null) {
                    mIntentField.setAccessible(true);
                    mIntentField.set(mLaunchActivityItem, targetIntent); //替换Intent
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }
    }


}
