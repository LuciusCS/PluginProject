package com.example.pluginproject.hook;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;



//必须要在AndroidManifest中进行注册，因为此Activity需要通过AMS进行检查
public class ProxyActivity extends AppCompatActivity {

    //用于表示TAG
    String TAG=ProxyActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG,"代理Activity");
    }
}
