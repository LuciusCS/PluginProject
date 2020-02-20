package com.example.plugin_package;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class PluginActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);
//this 会报错，因为插件没有安装，也没有插件环境，所以必须使用宿主环境
//        Toast.makeText(this,"插件",Toast.LENGTH_LONG).show();
        Toast.makeText(appActivity, "插件", Toast.LENGTH_LONG).show();


        //启动插件内部的Activity
        findViewById(R.id.start_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(appActivity, TestActivity.class);
                startActivity(intent);
            }
        });

        //启动插件内部的Service
        findViewById(R.id.start_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(new Intent(appActivity, TestService.class));
            }
        });

        findViewById(R.id.register_receive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("com.example.plugin_package.ACTION");
                registerReceiver(new MyReceiver(), intentFilter);

            }
        });

        findViewById(R.id.send_receive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction("com.example.plugin_package.ACTION");
                sendBroadcast(intent);

            }
        });
    }
}
