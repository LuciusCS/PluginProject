package com.example.pluginproject.placeholder;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.example.pluginproject.R;
import com.example.pluginproject.hook.HookActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /**
     * 加载插件
     * @param view
     */
    public void loadPlugin(View view) {
        PluginManager.getInstance(this).loadPlugin();
    }

    /**
     * 启动插件里面的Activity
     * @param view
     */
    public void startPluginActivity(View view) {

        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "p.apk");

        String path=file.getAbsolutePath();

        //获取插件包黎明的 Activity
        PackageManager packageManager=getPackageManager();
       PackageInfo packageInfo= packageManager.getPackageArchiveInfo(path,PackageManager.GET_ACTIVITIES);
        ActivityInfo activityInfo=packageInfo.activities[0];

        //占位，代理Activity
        Intent intent=new Intent(this,ProxyActivity.class);
        intent.putExtra("className",activityInfo.name);
        startActivity(intent);
    }

    public void startHook(View view) {
        startActivity(new Intent(this, HookActivity.class));
    }
}
