package com.example.pluginproject.application;

import android.app.Application;
import android.content.Intent;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.pluginproject.hook.ProxyActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;



import com.example.pluginproject.R;
import com.example.pluginproject.hook.ProxyActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import java.util.Map;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;


public class HookApplication extends Application {


    String TAG = HookApplication.class.getSimpleName();

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
//************** LoadedApk****************************
        try {
            hookLaunchActivity();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Hook失败" + e.getMessage());
        }


        try {
            customLoadedApkAction();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "pluginToApplication失败" + e.getMessage());
        }


//************** LoadedApk****************************
    }


    private void hookAndroid9AmsAction() throws Exception{
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

    /**
     * 要在执行AMS之前，替换在AndroidManifest里面配置的Activity
     *
     * 无论是哪一个版本都要执行此方法
     *
     */
    private void hookAmsAction() throws Exception {


        Class mIActivityManagerClass = Class.forName("android.app.IActivityManager");   //需要进行监听的接口

        //需要拿到ActivityManager对象，才能让动态代理里面的invoke正常执行下去
        //执行此方法的static public IActivityManager getDefault()  就能拿到IActivityManager

        Class mActivityManagerNativeClass2 = Class.forName("android.app.ActivityManagerNative");

        final Object mIActivityManager = mActivityManagerNativeClass2.getMethod("getDefault").invoke(null);

        /**
         * @2 要检测启动 startActivity的行为，所以需要使用动态代理
         */
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

                            Intent intent = new Intent(HookApplication.this, ProxyActivity.class);
                            intent.putExtra("actionIntent", ((Intent) args[2])); //把之前的TestActivity携带过去，后面进行还原
                            args[2] = intent;

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
//        mInstanceField.set(IActivityManagerSingleton, mIActivityManagerProxy); //为了拿到gDefault 替换是需要gDefault
    }

    //Android对public方法修改较少，主要修改私有的方法
    //Android 9 的Hook
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
        mCallbackField.set(mh, new MyCallbackAndroid9());

    }

    //Callback mCallback使用自己定义的callback
    private class MyCallbackAndroid9 implements android.os.Handler.Callback {

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
        Handler mH = (Handler) mHField.get(mActivityThread);

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

                    Object obj = msg.obj;  //本质是ActivityClientRecord
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


                        /**
                         * 在以下代码中，对插件和宿主进行区分
                         */

                        Field activityInfoField=obj.getClass().getDeclaredField("activityInfo");

                        activityInfoField.setAccessible(true);

                        ActivityInfo activityInfo= (ActivityInfo) activityInfoField.get(obj);

                        //加载插件的时机
                        if (actionIntent.getPackage()==null){  //证明是插件
                            activityInfo.applicationInfo.packageName=actionIntent.getComponent().getPackageName();
                            //Hook 拦截此 getPackageInfo 做自己的逻辑
                            hookGetPackageInfo();
                            
                            
                        }else {
                            //宿主
                            activityInfo.applicationInfo.packageName=actionIntent.getPackage();
                        }
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



    private Resources resources;
    private AssetManager assetManager;

    /**
     * //处理加载插件中的布局
     * Resources
     */

    private void doPluginLayoutLoad() throws Exception {
        assetManager = AssetManager.class.newInstance();

        //把插件的路径 给AssetManager

        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "p.apk");
        if (!file.exists()) {
            throw new FileNotFoundException("没有找到插件包");
        }
        Resources resources = getResources();  //拿到宿主的配置信息

        //执行， 才能把插件的路径加进去  public final int addAssetPath(String path) 方法才能把路径添加进去
        Method method = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
        method.setAccessible(true);
        method.invoke(assetManager, file.getAbsolutePath());

        //实例化此方法 final StringBlock[] ensureStringBlocks()
        Method ensureStringBlocksMethod = assetManager.getClass().getDeclaredMethod("ensureStringBlocks");
        ensureStringBlocksMethod.setAccessible(true);
        ensureStringBlocksMethod.invoke(assetManager);  //执行了ensureStringBlocks string.xml color.xml anim.xml 被初始化

        //专门加载插件资源
        resources = new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());
    }

    /**
     * 创建一个loadedApk.ClassLoader 添加到mPackage ，此LoadApk 专门用来加载插件Activity
     */
    private void customLoadedApkAction() throws Exception {

        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "p.apk");

        if (!file.exists()){
            throw  new FileNotFoundException("插件包不存在"+file.getAbsolutePath());
        }
        String pluginPath = file.getAbsolutePath();
        //mPackages 添加自定义的loadApk
        // final ArrayMap<String,WeakReference<LoadApk>> mPackages 添加自定义LoadedApk
        Class mActivityThreadClass = Class.forName("android.app.ActivityThread");

        //执行此方法 public static ActivityThread currentActivityThread() 拿到ActivityThread对象
        Object mActivityThread = mActivityThreadClass.getMethod("currentActivityThread").invoke(null);

        Field mPackagesField = mActivityThreadClass.getDeclaredField("mPackages");

        mPackagesField.setAccessible(true);

        //拿到mPackages对象
        Object mPackageObj = mPackagesField.get(mActivityThread);

        Map mPackages = (Map) mPackageObj;
        //自定义一个LoadApk, 根据系统创造loadApk的方式进行创造
        //执行 public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai,CompatibilityInfo compatInfo)

        Class mCompatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
        Field defaultField = mCompatibilityInfoClass.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
        defaultField.setAccessible(true);
        Object defaultObj = defaultField.get(null);

        //获取插件的ApplicationInfo
        ApplicationInfo applicationInfo=getApplicationInfoAction();
        Method mLoadedApkMethod = mActivityThreadClass.getMethod("getPackageInfoNoCheck", ApplicationInfo.class, mCompatibilityInfoClass);

        Object mLoadedApk = mLoadedApkMethod.invoke(mActivityThread, applicationInfo, defaultObj);

        //自定义加载器 加载插件
        File fileDir=getDir("pluginPathDir",Context.MODE_PRIVATE);
        //String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent
        //自定义插件的classLoader
        ClassLoader classLoader=new PluginClassLoader(pluginPath,fileDir.getAbsolutePath(),null,getClassLoader());

        Field mClassLoaderField = mLoadedApk.getClass().getDeclaredField("mClassLoader");
        mClassLoaderField.setAccessible(true);
        mClassLoaderField.set(mLoadedApk,classLoader);  //替换LoadApk中的ClassLoader

        //添加自定义的LoadedApk专门加载插件里面的class

        //最终的目标mPackages.put(插件的包名，插件的LoadApk)
        WeakReference weakReference = new WeakReference(mLoadedApk); //嵌入自定的LoadApk ——》 插件的
        mPackages.put(applicationInfo.packageName,weakReference);


    }

    /**
     * 获取ApplicationInfo
     *
     * @return
     * @throws Exception
     */
    private ApplicationInfo getApplicationInfoAction() throws Exception {
        //执行 public static ApplicationInfo getApplicationInfo 方法，获取ApplicationInfo

        Class mPackageParserClass = Class.forName("android.content.pm.PackageParser");

        Object mPackageParser = mPackageParserClass.newInstance();

        //内部类使用 $ 符号  generateApplicationInfo 方法的类类型
        Class $PackageClass = Class.forName("android.content.pm.PackageParser$Package");

        Class mPackageUserStateClass = Class.forName("android.content.pm.PackageUserState");

        Method mApplicationInfoMethod = mPackageParserClass.getMethod("generateApplicationInfo", $PackageClass,
                int.class, mPackageUserStateClass);


        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "p.apk");
        String pluginPath = file.getAbsolutePath();

        //执行public Package parsePackage(File packageFile,int flags) 方法，拿到Package
        Method mParsePackageMethod = mPackageParserClass.getMethod("parsePackage", File.class, int.class);
        Object mPackage = mParsePackageMethod.invoke(mPackageParser, file, PackageManager.GET_ACTIVITIES);


        //参数Package p,int flags, PackageUserState state
        ApplicationInfo applicationInfo = (ApplicationInfo)
                mApplicationInfoMethod.invoke(mPackageParser, mPackage,0, mPackageUserStateClass.newInstance());


//        //获取的ApplicationInfo就是插件的ApplicationInfo
        applicationInfo.publicSourceDir = pluginPath;
        applicationInfo.sourceDir = pluginPath;

        return applicationInfo;
    }

    //Hook拦截 getPackageInfo做自己的逻辑
    private void hookGetPackageInfo() {
        try {
            //sPackageManager 替换 我们自己的动态代理
            Class mActivityThreadClass = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField=mActivityThreadClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);


            Field sPackageManagerField = mActivityThreadClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            final Object packageManager=sPackageManagerField.get(null);

            /***
             * 动态代理
             */
            //使用动态代理替换成我们的逻辑
            Class mIPackageManagerClass = Class.forName("android.content.pm.IPackageManager");
            Object mIPackageManagerProxy=Proxy.newProxyInstance(getClassLoader(),
                    new Class[]{mIPackageManagerClass},  //要监听的接口
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if ("getPackageInfo".equals(method.getName())) {
                                //绕过PMS
                                return  new PackageInfo();
                            }
                            //让系统继续正常执行
                            return method.invoke(packageManager,args);
                        }
                    }
            );

            //因为sPackageManager是静态的所以可以使用null
            sPackageManagerField.set(null,mIPackageManagerProxy);
        }catch (Exception e){

        }

    }

    @Override
    public Resources getResources() {
        return resources == null ? super.getResources() : resources;
    }

    @Override
    public AssetManager getAssets() {
        return super.getAssets();
    }


}
