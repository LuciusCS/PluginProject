package com.example.pluginproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.stander.ReceiverInterface;

// 能够接收的 广播接收者
public class ProxyReceiver extends BroadcastReceiver {


    //插件里面MyReceive全类名
    private String className;

    public ProxyReceiver(String className) {
        this.className=className;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        //加载插件里面的MyReceiver
        try {
            Class<?> myReceiver = PluginManager.getInstance(context).getClassLoader().loadClass(className);

            //实例化class
            Object receiver = myReceiver.newInstance();

            ReceiverInterface receiverInterface=(ReceiverInterface)receiver;

            //执行插件里面的MyReceiver onReceive方法
            receiverInterface.onReceive(context,intent);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

    }
}
