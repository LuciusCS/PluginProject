package com.example.pluginproject.placeholder;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class PluginManager {

    private static final String TAG = PluginManager.class.getSimpleName();

    private static PluginManager pluginManager;

    private Context context;

    public static PluginManager getInstance(Context context) {
        if (pluginManager == null) {
            synchronized (PluginManager.class) {
                if (pluginManager == null) {
                    pluginManager = new PluginManager(context);
                }

            }
        }

        return pluginManager;
    }

    public PluginManager(Context context) {
        this.context = context;
    }

    private DexClassLoader dexClassLoader;
    private Resources resources;

    /**
     * 2.1 Activity class
     * 2.2 layout
     * 加载插件
     */


    public void loadPlugin() {

        try {
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "p.apk");
            if (!file.exists()) {
                Log.d(TAG, "插件包不存在");
                return;
            }

            /**
             * 下面加载插件里面的class
             */

            String pluginPath = file.getAbsolutePath();

            //dexClassLoader需要一个缓存目录  /data/data/当前应用功能的包名/pdir
            File fileDir = context.getDir("pDir", Context.MODE_PRIVATE);

            //Activity class
            dexClassLoader = new DexClassLoader(pluginPath, fileDir.getAbsolutePath(), null, context.getClassLoader());


            /**
             * 下面加载插件中的layout
             */

            //加载资源
//            AssetManager assetManager=context.getAssets();
            AssetManager assetManager=AssetManager.class.newInstance();

            //执行此方法，为了将插件包的路径添加进去
            //public final int addAssetPath(String path)
            Method addAsserMethod=assetManager.getClass().getMethod("addAssetPath",String.class);
            addAsserMethod.invoke(assetManager,pluginPath);

            Resources r=context.getResources();   //宿主的资源配置信息

            //特殊的Resources， 加载插件李敏的资源的Resources
            resources=new Resources(assetManager,r.getDisplayMetrics(),r.getConfiguration());   //参数2 3资源的配置信息


        } catch (Exception e) {

        }

    }

    public ClassLoader getClassLoader(){

        return dexClassLoader;

    }

    public Resources getResources() {
        return resources;
    }
}
