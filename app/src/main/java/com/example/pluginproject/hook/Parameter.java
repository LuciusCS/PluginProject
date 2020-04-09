package com.example.pluginproject.hook;

/**
 * 用于做标记的类
 */
public interface Parameter {

    String TARGET_INTENT="targetIntentMyTest"; // TODO 目标Acitivy

    int EXECUTE_TRANSACTION=159; //todo 在ActivityThread中即将还要去实例Activity 会经过此Handler标记 适用于高版本

    int LAUNCH_ACTIVITY=100; //todo 在ActivityThread中即将还要去实例化Activity 会经过此Handler标记

    String PLUGIN_FILE_NAME="PLUGIN-DEBUG-APK";  //todo 插件名


}
