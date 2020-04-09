package com.example.pluginproject.hook;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.example.pluginproject.application.MyApplication;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * 宿主和插件DexElement进行合并
 */
public class DexElementFuse {
    // private DexClassLoader dexClassLoader;
    private Resources resources = null;
    private AssetManager assetManager = null;

    //宿主和插件的DexElement融合
    public void mainPluginFuse(Context mContext) throws Exception {
        //租住的dexElement
        //Object mainDexElements=getDexElements(mContext.getClassLoader());
        Class mBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        Field pathListField = mBaseDexClassLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object mDexPathList = pathListField.get(mContext.getClassLoader());
        Field dexElementsField = mDexPathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        Object mainDexElements = dexElementsField.get(mDexPathList);

        //todo 插件的dexElements

        /* File file=new File(Environment.getExternalStorageDirectory()+File.separator+"p.apk");
           if(file.exists()==false){
                return;
           }
         */

        File fileDir = mContext.getDir("pDir", Context.MODE_PRIVATE);

        DexClassLoader dexClassLoader = new DexClassLoader(MyApplication.pluginPath, fileDir.getAbsolutePath(), null, mContext.getClassLoader());

        // Object pluginDexElements=getDexElements(dexClassLoader);
        Class mBaseDexClassLoaderClass2 = Class.forName("dalvik.system.BaseDexClassLoader");
        Field pathListField2 = mBaseDexClassLoaderClass2.getDeclaredField("pathList");
        pathListField2.setAccessible(true);
        Object mDexPathList2 = pathListField2.get(dexClassLoader);
        Field dexElementField2 = mDexPathList2.getClass().getDeclaredField("dexElements");
        dexElementField2.setAccessible(true);
        Object pluginDexElements = dexElementField2.get(mDexPathList2);

        // todo 创造出新的 newDexElements
        int mainLen = Array.getLength(mainDexElements);
        int pluginLen = Array.getLength(pluginDexElements);
        int newDexElementsLength = mainLen + pluginLen;
        Object newDexElements = Array.newInstance(mainDexElements.getClass().getComponentType(), newDexElementsLength);
        //进行融合
        for (int i = 0; i < newDexElementsLength; i++) {
            //先融合宿主
            if (i < mainLen) {
                Array.set(newDexElements, i, Array.get(mainDexElements, i));
            } else {
                //融合插件
                Array.set(newDexElements, i, Array.get(pluginDexElements, i - mainLen));
            }

        }
        //新的替换到宿主中去
        dexElementsField.set(mDexPathList, newDexElements);
        loadResource(mContext);

    }

    //宿主的Elements和插件的dexElements 代码类型，可以抽取成方法，代码未做
    private Object getDexElements(ClassLoader classLoader) throws Exception {
        Class mBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        Field pathListField = mBaseDexClassLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object mDexPathList = pathListField.get(classLoader);
        Field dexElementsField = mDexPathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        return dexElementsField.get(mDexPathList);
    }


    private void loadResource(Context mContext) throws Exception {
        Resources r = mContext.getResources();
        assetManager = AssetManager.class.newInstance();
        Method addAssetpathMethod = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
        addAssetpathMethod.setAccessible(true);
        //File file=new File(Environment.getExternalStorageDirectory()+File.separator+"p.apk");
        addAssetpathMethod.invoke(addAssetpathMethod, MyApplication.pluginPath);

        resources = new Resources(assetManager, r.getDisplayMetrics(), r.getConfiguration());
    }

    public Resources getResources() {
        return resources;
    }
}
