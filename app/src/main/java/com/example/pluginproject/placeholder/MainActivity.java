package com.example.pluginproject.placeholder;



import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.pluginproject.R;
import com.example.pluginproject.hook.ProxyActivity;
import com.example.pluginproject.hook.TestActivity;

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



    public void startTest(View view) {
//        startActivity(new Intent(this, ProxyActivity.class));
    //宿主中，去启动插件里面的PluginActivity --
        Intent intent=new Intent();

        intent.setComponent(new ComponentName("com.example.plugin_package","com.example.plugin_package.PluginActivity"));

        startActivity(intent);
    }
}
