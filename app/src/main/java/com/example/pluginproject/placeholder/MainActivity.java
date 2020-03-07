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
import com.example.pluginproject.hook.TestActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }








    public void startTest(View view) {
        startActivity(new Intent(this, TestActivity.class));
    }
}
