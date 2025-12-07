package com.example.demo_323;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 应用配置类 - 统一管理所有配置项
 * 配置文件位置: assets/config.properties
 */
public class AppConfig {
    private static final String TAG = "AppConfig";
    private static final String CONFIG_FILE = "config.properties";
    
    private static AppConfig instance;
    private Properties properties;
    
    // 默认值
    private static final int DEFAULT_MAX_RETRY = 3;
    private static final int DEFAULT_MAX_DIALOG_RETRY = 3;
    private static final long DEFAULT_PAGE_LOAD_TIMEOUT = 5000;
    private static final long DEFAULT_DEFAULT_DELAY = 2000;
    private static final long DEFAULT_SPLASH_DELAY = 2000;
    private static final float DEFAULT_SWITCH_X_RATIO = 0.88f;
    private static final String DEFAULT_PREFS_NAME = "AutoPrivacyPrefs";
    private static final String DEFAULT_SCRIPTS_PATH = "scripts/";
    
    private AppConfig(Context context) {
        properties = new Properties();
        loadConfig(context);
    }
    
    public static AppConfig getInstance(Context context) {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    private void loadConfig(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(CONFIG_FILE);
            properties.load(inputStream);
            inputStream.close();
            Log.i(TAG, "配置文件加载成功");
        } catch (IOException e) {
            Log.w(TAG, "配置文件加载失败，使用默认配置: " + e.getMessage());
            // 使用默认配置
            setDefaultProperties();
        }
    }
    
    private void setDefaultProperties() {
        properties.setProperty("max_retry", String.valueOf(DEFAULT_MAX_RETRY));
        properties.setProperty("max_dialog_retry", String.valueOf(DEFAULT_MAX_DIALOG_RETRY));
        properties.setProperty("page_load_timeout", String.valueOf(DEFAULT_PAGE_LOAD_TIMEOUT));
        properties.setProperty("default_delay", String.valueOf(DEFAULT_DEFAULT_DELAY));
        properties.setProperty("splash_delay", String.valueOf(DEFAULT_SPLASH_DELAY));
        properties.setProperty("switch_x_ratio", String.valueOf(DEFAULT_SWITCH_X_RATIO));
        properties.setProperty("prefs_name", DEFAULT_PREFS_NAME);
        properties.setProperty("scripts_path", DEFAULT_SCRIPTS_PATH);
    }
    
    // Getter 方法
    
    /**
     * 获取最大重试次数
     */
    public int getMaxRetry() {
        return getIntProperty("max_retry", DEFAULT_MAX_RETRY);
    }
    
    /**
     * 获取对话框最大重试次数
     */
    public int getMaxDialogRetry() {
        return getIntProperty("max_dialog_retry", DEFAULT_MAX_DIALOG_RETRY);
    }
    
    /**
     * 获取页面加载超时时间（毫秒）
     */
    public long getPageLoadTimeout() {
        return getLongProperty("page_load_timeout", DEFAULT_PAGE_LOAD_TIMEOUT);
    }
    
    /**
     * 获取默认延迟时间（毫秒）
     */
    public long getDefaultDelay() {
        return getLongProperty("default_delay", DEFAULT_DEFAULT_DELAY);
    }
    
    /**
     * 获取启动页延迟时间（毫秒）
     */
    public long getSplashDelay() {
        return getLongProperty("splash_delay", DEFAULT_SPLASH_DELAY);
    }
    
    /**
     * 获取开关X轴点击位置比例（相对于屏幕宽度）
     */
    public float getSwitchXRatio() {
        return getFloatProperty("switch_x_ratio", DEFAULT_SWITCH_X_RATIO);
    }
    
    /**
     * 获取SharedPreferences名称
     */
    public String getPrefsName() {
        return getStringProperty("prefs_name", DEFAULT_PREFS_NAME);
    }
    
    /**
     * 获取脚本文件路径
     */
    public String getScriptsPath() {
        return getStringProperty("scripts_path", DEFAULT_SCRIPTS_PATH);
    }
    
    /**
     * 获取应用启动后等待时间（毫秒）
     */
    public long getAppLaunchWaitTime() {
        return getLongProperty("app_launch_wait_time", 1500);
    }
    
    /**
     * 获取点击后等待时间（毫秒）
     */
    public long getClickWaitTime() {
        return getLongProperty("click_wait_time", 1000);
    }
    
    /**
     * 获取返回操作等待时间（毫秒）
     */
    public long getTurnbackWaitTime() {
        return getLongProperty("turnback_wait_time", 1500);
    }
    
    /**
     * 获取对话框处理等待时间（毫秒）
     */
    public long getDialogHandleWaitTime() {
        return getLongProperty("dialog_handle_wait_time", 2000);
    }
    
    /**
     * 获取滚动等待时间（毫秒）
     */
    public long getScrollWaitTime() {
        return getLongProperty("scroll_wait_time", 1500);
    }
    
    /**
     * 获取最大滚动次数
     */
    public int getMaxScrollCount() {
        return getIntProperty("max_scroll_count", 5);
    }
    
    /**
     * 获取滚动起始位置比例（相对于屏幕高度）
     */
    public float getScrollStartRatio() {
        return getFloatProperty("scroll_start_ratio", 0.7f);
    }
    
    /**
     * 获取滚动结束位置比例（相对于屏幕高度）
     */
    public float getScrollEndRatio() {
        return getFloatProperty("scroll_end_ratio", 0.4f);
    }
    
    // 辅助方法
    
    private int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            Log.w(TAG, "解析配置项失败: " + key + ", 使用默认值: " + defaultValue);
            return defaultValue;
        }
    }
    
    private long getLongProperty(String key, long defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            Log.w(TAG, "解析配置项失败: " + key + ", 使用默认值: " + defaultValue);
            return defaultValue;
        }
    }
    
    private float getFloatProperty(String key, float defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Float.parseFloat(value) : defaultValue;
        } catch (NumberFormatException e) {
            Log.w(TAG, "解析配置项失败: " + key + ", 使用默认值: " + defaultValue);
            return defaultValue;
        }
    }
    
    private String getStringProperty(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? value : defaultValue;
    }
}

