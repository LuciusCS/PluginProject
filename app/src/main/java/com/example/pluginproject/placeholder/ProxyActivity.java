package com.example.pluginproject.placeholder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.example.stander.ActivityInterface;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 代理Activity ,代理插件里面的Activity
 */
public class ProxyActivity extends Activity {

    @Override
    public Resources getResources() {
        return PluginManager.getInstance(this).getResources();
    }

    @Override
    public ClassLoader getClassLoader() {
        return PluginManager.getInstance(this).getClassLoader();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //真正的加载插件里面的Activity
        String className=getIntent().getStringExtra("className");

        try {
            Class mPluginActivityClass=getClassLoader().loadClass(className);
            //实例化，插件包中的Activity
            Constructor constructor=mPluginActivityClass.getConstructor(new Class[]{});
            Object mPluginActivity=constructor.newInstance(new Object[]{});

            ActivityInterface activityInterface=(ActivityInterface)mPluginActivity;

            //注入
            activityInterface.insertAppContext(this);


            Bundle bundle=new Bundle();
            bundle.putString("appName","宿主传递过来的信息");

            //执行插件里面的onCreate方法
            activityInterface.onCreate(bundle);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void startActivity(Intent intent) {

       String className= intent.getStringExtra("className");


        Intent proxyIntent=new Intent(this,ProxyActivity.class);
        proxyIntent.putExtra("className",className);  //TestActivity

        //要给TestActivity进展
       super.startActivity(proxyIntent);

    }

    @Override
    public ComponentName startService(Intent service) {
        String className= service.getStringExtra("className");

        Intent intentNew=new Intent(this,ProxyService.class);
        intentNew.putExtra("className",className);  //TestService全类名
        return super.startService(intentNew);
    }


    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        //在宿主注册广播
        String className= receiver.getClass().getName();  //MyReceive的全类名

        Intent intentNew=new Intent(this,ProxyService.class);
        intentNew.putExtra("className",className);  //TestService全类名

        return super.registerReceiver(new ProxyReceiver(className), filter);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        super.sendBroadcast(intent);
    }
}
