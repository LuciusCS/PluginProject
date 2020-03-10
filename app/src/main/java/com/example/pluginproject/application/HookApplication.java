package com.example.pluginproject.application;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.example.pluginproject.hook.ProxyActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.PropertyResourceBundle;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;


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

        try {
            pluginToApplication();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"pluginToApplication失败"+e.getMessage());
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

    /**
     * 把插件的dexElement 和宿主的dexElement 插件融为一体
     * @throws Exception
     */
    private void pluginToApplication() throws Exception{

        //第一步： 找到宿主的 dexElements得到此对象

        PathClassLoader pathClassLoader= (PathClassLoader) this.getClassLoader(); //本质就是PathClassLoader
        Class mBaseDexClassLoaderClass=Class.forName("dalvik.system.BaseDexClassLoader");
        // private final DexPathList pathlist;
        Field pathListField=mBaseDexClassLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object mDexPathList=pathListField.get(pathClassLoader);

        Field dexElementsField = mDexPathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        //本质是Element[] dexElements
        Object dexElements = dexElementsField.get(mDexPathList);



        //第二步： 找到插件的dexElements得到此对象

        File file=new File(Environment.getExternalStorageDirectory()+File.separator+"p.apk");
        if (!file.exists()){
            throw  new FileNotFoundException("没有找到插件包");
        }

        String pluginPath=file.getAbsolutePath();
        File fileDir=this.getDir("pluginDir", Context.MODE_PRIVATE);  //data/data/包名/pluginDir/

        DexClassLoader dexClassLoader=new DexClassLoader(pluginPath,fileDir.getAbsolutePath(),null,getClassLoader());

        Class mBaseDexClassLoaderClassPlugin=Class.forName("dalvik.system.BaseDexClassLoader");
        // private final DexPathList pathlist;
        Field pathListFieldPlugin=mBaseDexClassLoaderClassPlugin.getDeclaredField("pathList");
        pathListFieldPlugin.setAccessible(true);
        Object mDexPathListPlugin=pathListFieldPlugin.get(dexClassLoader);

        Field dexElementsFieldPlugin = mDexPathListPlugin.getClass().getDeclaredField("dexElements");
        dexElementsFieldPlugin.setAccessible(true);
        //本质是Element[] dexElements
        Object dexElementPlugin = dexElementsFieldPlugin.get(mDexPathListPlugin);


        //第三步：创建出新的dexElements[]

        int mainDexLength=Array.getLength(dexElements);
        int pluginDexLength=Array.getLength(dexElementPlugin);
        int sumDexLength=mainDexLength + pluginDexLength;

        //参数1：int[] 、String[] ，这里需要Element[]
        //参数2：数组对象的长度
        //newElements的本质是newDexElements
        Object newDexElements = Array.newInstance(dexElements.getClass().getComponentType(), sumDexLength);//创建数组参数

        //第四步：宿主dexElements+插件dexElements = ——》新的dexElements
        for (int i=0;i<sumDexLength;i++){
            //先融合宿主
            if (i<mainDexLength){
                //参数一：新要融合的容器 --- newElements
                Array.set( newDexElements,i,Array.get(dexElements,i));
            }else{
                //再融合插件的
                Array.set( newDexElements,i,Array.get(dexElementPlugin,i-mainDexLength));
            }

        }

        //第五步：把新的DexElements，设置到宿主中去
        //宿主
        dexElementsField.set(mDexPathList,newDexElements);




    }

    private Resources resources;
    private AssetManager assetManager;

    /**
     *     //处理加载插件中的布局
     *     Resources
     */

    private void doPluginLayoutLoad() throws Exception{
        assetManager=AssetManager.class.newInstance();

        //把插件的路径 给AssetManager

        File file=new File(Environment.getExternalStorageDirectory()+File.separator+"p.apk");
        if (!file.exists()){
            throw  new FileNotFoundException("没有找到插件包");
        }
        Resources resources=getResources();  //拿到宿主的配置信息

        //执行， 才能把插件的路径加进去  public final int addAssetPath(String path) 方法才能把路径添加进去
        Method method = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
        method.setAccessible(true);
        method.invoke(assetManager,file.getAbsolutePath());

        //实例化此方法 final StringBlock[] ensureStringBlocks()
        Method ensureStringBlocksMethod = assetManager.getClass().getDeclaredMethod("ensureStringBlocks");
        ensureStringBlocksMethod.setAccessible(true);
        ensureStringBlocksMethod.invoke(assetManager);  //执行了ensureStringBlocks string.xml color.xml anim.xml 被初始化

        //专门加载插件资源
        resources=new Resources(assetManager, resources.getDisplayMetrics(),resources.getConfiguration());
    }

    @Override
    public Resources getResources() {
        return resources==null?super.getResources():resources;
    }

    @Override
    public AssetManager getAssets() {
        return super.getAssets();
    }
}
