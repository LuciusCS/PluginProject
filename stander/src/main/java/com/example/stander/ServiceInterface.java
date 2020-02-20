package com.example.stander;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;

public interface ServiceInterface {
    /**
     * 将宿主的环境给插件
     * @param
     */
    public void insertAppContext(Service appService);

    public void onCreate() ;


    public int onStartCommand(Intent intent, int flags, int startId) ;



    public void onDestroy() ;

}
