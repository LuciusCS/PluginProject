package com.example.plugin_package;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Resources getResources() {
        if (getApplication()!=null&& getApplication().getResources()!=null){
            return getApplication().getResources();
        }

        return super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if (getApplication()!=null&& getApplication().getAssets()!=null){
            return getApplication().getAssets();
        }
        return super.getAssets();
    }
}
