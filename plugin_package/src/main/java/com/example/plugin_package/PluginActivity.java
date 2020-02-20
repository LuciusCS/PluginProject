package com.example.plugin_package;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
        Toast.makeText(appActivity,"插件",Toast.LENGTH_LONG).show();

        findViewById(R.id.start_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(appActivity,TestActivity.class);
                startActivity(intent);
            }
        });

    }
}
