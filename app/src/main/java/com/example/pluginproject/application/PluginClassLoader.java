package com.example.pluginproject.application;

import dalvik.system.DexClassLoader;

//加载插件里面的class的加载器
public class PluginClassLoader extends DexClassLoader {


    /**
     *
     * @param dexPath 路径
     * @param optimizedDirectory 缓存路径
     * @param librarySearchPath C++ 库的路径
     * @param parent
     */
    public PluginClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
    }
}
