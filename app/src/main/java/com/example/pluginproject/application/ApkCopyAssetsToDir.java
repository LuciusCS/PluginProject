package com.example.pluginproject.application;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class ApkCopyAssetsToDir {

    // 将文件从assets目录拷贝到 app的Cache目录
    public static String copyAssetToCache(Context context,String fileName) {
        //app的缓存目录--》 会默认在Cache 目录
        File cacheDir=context.getCacheDir();
        if (!cacheDir.exists()){
            cacheDir.mkdir();  //如果没有缓存目录就创建缓存目录
        }

        File outPath=new File(cacheDir,fileName);   //创建输出文件的位置
        if (outPath.exists()){
            outPath.delete();   //如果该文件已经存在，就直接删掉
        }

        InputStream is=null;
        FileOutputStream fos=null;


        //创建文件，如果创建成功，直接返回true
        try {
            boolean res=outPath.createNewFile();
            if (res){
                is=context.getAssets().open(fileName);
                fos=new FileOutputStream(outPath);
                byte[] buf=new byte[is.available()];  //缓存区

                int byteCount;

                //开始循环读取
                while ((byteCount=is.read(buf))!=-1){
                    fos.write(buf,0,byteCount);
                }

                return outPath.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //需要关闭资源
            try {
                fos.close();
                is.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return null;
    }
}

