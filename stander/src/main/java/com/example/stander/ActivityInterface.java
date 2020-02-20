package com.example.stander;

import android.app.Activity;
import android.os.Bundle;

/**
 * Activity 标准
 */
public interface ActivityInterface {

    /**
     * 将宿主的环境给插件
     * @param appActivity
     */
   public void insertAppContext(Activity appActivity);

    public void onCreate(Bundle savedInstanceState);

    public void onStart();

    public void onResume();

    public void onDestroy();
}
