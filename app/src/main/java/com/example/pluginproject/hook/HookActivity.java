package com.example.pluginproject.hook;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pluginproject.R;
import com.example.pluginproject.placeholder.MainActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class HookActivity extends AppCompatActivity {

    private final String TAG = HookActivity.class.getSimpleName();

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hook);

        TextView textView = findViewById(R.id.click_id);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e(TAG,((TextView)v).getText().toString());


            }
        });

        try {
            hook(textView);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Hook失败");
        }
    }

    //在不改变代码的情况下，改变getText
    public void hook(View view) throws Exception {


        Class mViewClass = Class.forName("android.view.View");
        Method getListenerInfoMethod = mViewClass.getDeclaredMethod("getListenerInfo");
        getListenerInfoMethod.setAccessible(true);

        //执行方法
        Object mListenerInfo = getListenerInfoMethod.invoke(view);

        //替换 把 public OnClickListener mOnClickListener; 替换成我们自己的
        Class mListenerInfoClass = Class.forName("android.view.View$ListenerInfo");

        Field mListenerClassField=mListenerInfoClass.getField("mOnClickListener");

        final Object mOnClickListenerObj =mListenerClassField .get(mListenerInfo);

        //1. 监听 onClick  当用户点击 TextView时，要先进行拦截
        //动态代理
        //OnClickListener
        Object mOnClickListenerProxy = Proxy.newProxyInstance(MainActivity.class.getClassLoader(),  //加载器
                new Class[]{View.OnClickListener.class},   //要监听的接口，监听什么接口，就返回什么借口

                new InvocationHandler() {  //监听接口里面的回调

                    /**
                     *  void onClick(View v)
                     *  onClick ----> Method
                     *  View v ----> Object[] args
                     *
                     * @param proxy
                     * @param method
                     * @param args
                     * @return
                     * @throws Throwable
                     */
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        //加入自己的逻辑
                        Log.e(TAG,"拦截成功");


                        TextView textView=new TextView(HookActivity.this);
                        textView.setText("测试 ");

                        //让系统程序片段 -- 继续往下执行


                        return method.invoke(mOnClickListenerObj,textView);
                    }
                });


        //把 mOnClickListener换成自己写的动态代理
        mListenerClassField.set(mListenerInfo,mOnClickListenerProxy);


    }


}
