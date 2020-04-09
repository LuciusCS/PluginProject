package com.example.pluginproject.application;

import android.os.Build;

public class AndroidSDKVersion {
    /***
         * 应该使用枚举类型
         *
          * API level 21 ---- Android 5.0
          * API level 22 ---- Android 5.1
          * API level 23 ---- Android 6.0
          * API level 24 ---- Android 7.0
          * API level 25 ---- Android 7.1.1
          * API level 26 ---- Android 8.0
          * API level 27 ---- Android 8.1
          * API level 28 ---- Android 9.0
          *
          */


           /***
      * API level 26 ---- Android 8.0
      * API level 27 ---- Android 8.1
      * API level 28 ---- Android 9.0
      */

            /**
      * 判断当前版本 26 27 28
      */

            public static boolean isAndroidOS_26_27_28(){
                int V= Build.VERSION.SDK_INT;
                if ((V>26||V==26)&&(V<28||V==28)){
                        return true;
                   }
                return false;
            }



            /***
      * API level 21 ---- Android 5.0
      * API level 22 ---- Android 5.1
      * API level 23 ---- Android 6.0
     * API level 24 ---- Android 7.0
      * API level 25 ---- Android 7.1.1
      */
           /**
     * 判断当前版本 21 22 23 24 25 以及 21 版本以下的
      */
           public static boolean isAndroidOS_21_22_23_24_25(){
                int V= Build.VERSION.SDK_INT;
                if (V<26){
                        return true;
                   }
                return false;
            }

}
