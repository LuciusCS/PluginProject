package com.example.pluginproject.hook;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.example.pluginproject.application.AndroidSDKVersion;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 在即将需要加载的时候，需要将ProxyActivity换成待启动的Activity
 */
public class ActivityThreadmHRestore {

    public static void mActivityThreadmHAction(Context context) throws Exception{
        if (AndroidSDKVersion.isAndroidOS_26_27_28()){
            do_26_27_28_mHRestore();
        }else if (AndroidSDKVersion.isAndroidOS_21_22_23_24_25()){
            do_21_22_23_24_25_mHRestore();
        }else {
            throw new IllegalStateException("未适配该系统版本");
        }


    }



    private static void do_26_27_28_mHRestore() throws Exception{
        //@1
        Class mActivityThreadClass=Class.forName("android.app.ActivityThread");
        Object mActivityThread=mActivityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field mHField=mActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Object mH=mHField.get(mActivityThread);

        Field mCallbackField= Handler.class.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);

        //@1 @2
        //将系统的Handler.Callback 替换成 Custom_26_27_28_Callback
        mCallbackField.set(mH,new Custom_26_27_28_Callback());

    }

    private static class Custom_26_27_28_Callback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {

            if (Parameter.EXECUTE_TRANSACTION==msg.what){
                //final ClientTransaction transaction=(ClientTransaction)msg.obj;
                //mTransactionExecutor.execute(transaction);
                Object mClientTransaction=msg.obj;

                //@1
                //Field mActivityCallbackField =mClientTransaction.getClass().getDeclaredField("mActivityCallbacks");

                try {
                    Class mClientTransactionClass=Class.forName("android.app.servertransaction.ClientTransaction");

                    Field mActivityCallbackField=mClientTransactionClass.getDeclaredField("mActivityCallbacks");

                    mActivityCallbackField.setAccessible(true);

                    //List<ClientTransactionItem>mActivityCallbacks;
                    List mActivityCallbacks=(List)mActivityCallbackField.get(mClientTransaction);

                    //todo 需要进行判断
                    if (mActivityCallbacks.size()==0){
                        return false;
                    }

                    Object mLaunchActivityItem=mActivityCallbacks.get(0);

                    Class mLaunchActivityItemClass=Class.forName("android.app.servertransaction.LaunchActivityItem");

                    //todo 需要判断
                    if (!mClientTransactionClass.isInstance(mLaunchActivityItem)){
                        return false;
                    }

                    Field mIntentField=mLaunchActivityItemClass.getDeclaredField("mIntent");
                    mIntentField.setAccessible(true);

                    //@2 需要拿到真实的Intent
                    Intent proxyIntent=(Intent)mIntentField.get(mLaunchActivityItem);

                    Intent targetIntent=proxyIntent.getParcelableExtra(Parameter.TARGET_INTENT);

                    if (targetIntent!=null){
                        mIntentField.set(mLaunchActivityItem,targetIntent);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }



            return false;
        }
    }


    /**
     * 专门给21_22_23_24_25  系统版本做还原操作的
     */
    private  final static void do_21_22_23_24_25_mHRestore() throws Exception{
        Class mActivityThreadClass=Class.forName("android.app.ActivityThread");
        Field msCurrentActivityThreadField=mActivityThreadClass.getDeclaredField("sCurrentActivityThread");
        msCurrentActivityThreadField.setAccessible(true);
        Object mActivityThread=msCurrentActivityThreadField.get(null);

        //获取 @1
        Field mHField=mActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Handler mH=(Handler)mHField.get(mActivityThread);

        Field mCallbackField=Handler.class.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);

        //@1 @2
        mCallbackField.set(mH,new Custom_21_22_23_24_25_Callback());

    }

    //@2
    private static final class Custom_21_22_23_24_25_Callback implements Handler.Callback{

        @Override
        public boolean handleMessage(Message msg) {

            if (Parameter.LAUNCH_ACTIVITY==msg.what){
                Object mActivityClientRecord=msg.obj;
                    try {
                        Field intentField=mActivityClientRecord.getClass().getDeclaredField("intent");
                        intentField.setAccessible(true);
                        Intent proxyIntent=(Intent)intentField.get(mActivityClientRecord);
                        //todo 还原操作
                        Intent targetIntent=proxyIntent.getParcelableExtra(Parameter.TARGET_INTENT);
                        if (targetIntent!=null){
                            //通过Component的方式会更好一些，但必须要通过Component的方式进行启动
                            // proxyIntent.setComponent(target.getComponent());

                            //反射的方式
                            intentField.set(mActivityClientRecord,targetIntent);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

            }

            return false;
        }
    }



}
