package com.privacy.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

public class SplashActivity extends Activity {
    private AppConfig config;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化配置
        config = AppConfig.getInstance(this);
        
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // 设置启动页面布局
        setContentView(R.layout.activity_splash);
        
        // 延迟跳转到主界面
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 启动主界面
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                // 结束启动页面
                finish();
            }
        }, config.getSplashDelay());
    }
}