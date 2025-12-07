package com.privacy.assistant;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity implements AppListAdapter.OnAppSelectedListener {
    private static final String TAG = "MainActivity";
    
    private ListView appListView;
    private Button startProcessButton;
    private AppListAdapter appListAdapter;
    private List<AppInfo> appList;
    private AppConfig config;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化配置
        config = AppConfig.getInstance(this);

        appListView = findViewById(R.id.app_list_view);
        startProcessButton = findViewById(R.id.start_process_button);
        Button selectAllButton = findViewById(R.id.select_all_button);
        Button deselectAllButton = findViewById(R.id.deselect_all_button);

        appList = new ArrayList<>();
        appListAdapter = new AppListAdapter(this, appList);
        appListView.setAdapter(appListAdapter);
        
        // 设置ListView的divider
        appListView.setDivider(null);
        appListView.setDividerHeight(0);

        loadAppList();

        startProcessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startProcessing();
            }
        });
        
        // 设置应用选择监听器
        appListAdapter.setOnAppSelectedListener(this);

        // 全选按钮点击事件
        selectAllButton.setOnClickListener(v -> {
            for (AppInfo appInfo : appList) {
                appInfo.isSelected = true;
            }
            appListAdapter.notifyDataSetChanged();
        });

        // 取消全选按钮点击事件
        deselectAllButton.setOnClickListener(v -> {
            for (AppInfo appInfo : appList) {
                appInfo.isSelected = false;
            }
            appListAdapter.notifyDataSetChanged();
        });
    }
    
    
    
    private void loadAppList() {
        // 获取已安装的应用列表
        List<AppInfo> installedApps = getInstalledApps();
        // 获取支持的应用列表（有脚本的）
        List<AppInfo> supportedApps = filterSupportedApps(installedApps);
        
        // 将支持的应用放在前面
        Collections.sort(supportedApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo app1, AppInfo app2) {
                return app1.appName.compareToIgnoreCase(app2.appName);
            }
        });
        
        Collections.sort(installedApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo app1, AppInfo app2) {
                return app1.appName.compareToIgnoreCase(app2.appName);
            }
        });
        
        // 合并列表，支持的应用在前
        appList.clear();
        appList.addAll(supportedApps);
        // 添加不支持的应用（避免重复）
        for (AppInfo app : installedApps) {
            boolean isSupported = false;
            for (AppInfo supportedApp : supportedApps) {
                if (supportedApp.packageName.equals(app.packageName)) {
                    isSupported = true;
                    break;
                }
            }
            if (!isSupported) {
                appList.add(app);
            }
        }
        
        appListAdapter.notifyDataSetChanged();
        
        // 检查是否有应用
        if (appList.isEmpty()) {
            Toast.makeText(this, "未找到已安装的应用", Toast.LENGTH_SHORT).show();
        }
    }
    
    private List<AppInfo> getInstalledApps() {
        List<AppInfo> appInfoList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        for (ApplicationInfo app : apps) {
            // 过滤掉系统应用
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                AppInfo appInfo = new AppInfo();
                appInfo.appName = app.loadLabel(pm).toString();
                appInfo.packageName = app.packageName;
                appInfo.isSupported = false;
                appInfo.isSelected = false;
                // 获取应用图标
                appInfo.appIcon = app.loadIcon(pm);
                appInfoList.add(appInfo);
            }
        }
        
        return appInfoList;
    }
    
    private List<AppInfo> filterSupportedApps(List<AppInfo> apps) {
        List<AppInfo> supportedApps = new ArrayList<>();
        
        for (AppInfo app : apps) {
            if (isJsonExist(app.packageName)) {
                app.isSupported = true;
                supportedApps.add(app);
                Log.d(TAG, "支持的应用: " + app.appName + " (" + app.packageName + ")");
            }
        }
        
        return supportedApps;
    }
    
    private boolean isJsonExist(String packageName) {
        try {
            InputStream is = getAssets().open(config.getScriptsPath() + packageName + ".json");
            is.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    @Override
    public void onAppSelected(AppInfo appInfo, boolean isSelected) {
        // 这个方法会在AppListAdapter中被调用
        // 我们不需要在这里做任何特殊处理，因为状态已经在adapter中更新了
    }
    
    private void startProcessing() {
        List<AppInfo> selectedApps = new ArrayList<>();
        for (AppInfo app : appList) {
            if (app.isSelected) {
                selectedApps.add(app);
            }
        }
        
        if (selectedApps.isEmpty()) {
            Toast.makeText(this, "请至少选择一个应用", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 这里应该启动无障碍服务并处理选中的应用
        Toast.makeText(this, "开始处理 " + selectedApps.size() + " 个应用", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "开始处理以下应用:");
        for (AppInfo app : selectedApps) {
            Log.d(TAG, "- " + app.appName + " (" + app.packageName + ")");
        }
        
        // 创建一个包含选中应用包名的列表
        List<String> selectedPackageNames = new ArrayList<>();
        for (AppInfo app : selectedApps) {
            selectedPackageNames.add(app.packageName);
        }
        
        // 将选定的应用列表保存到SharedPreferences，供AutoPrivacyService使用
        android.content.SharedPreferences prefs = getSharedPreferences(config.getPrefsName(), MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        // 清除之前的数据
        editor.clear();
        // 保存选定的应用数量
        editor.putInt("selected_app_count", selectedPackageNames.size());
        // 保存每个应用的包名
        for (int i = 0; i < selectedPackageNames.size(); i++) {
            editor.putString("selected_app_" + i, selectedPackageNames.get(i));
        }
        editor.apply();
        
        // 启动无障碍服务
        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }
}