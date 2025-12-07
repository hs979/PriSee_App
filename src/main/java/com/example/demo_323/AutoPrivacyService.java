package com.example.demo_323;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AutoPrivacyService extends AccessibilityService {

    private Queue<String> appQueue = new LinkedList<>();
    private String currentPackage;
    private PrivacyScript currentScript;
    private Handler handler = new Handler(Looper.getMainLooper());
    private AppConfig config;

    // 状态控制
    private boolean isWaitingForAppLaunch = false;
    private int retryCount = 0;
    // 新增弹窗处理相关变量
    private boolean isHandlingDialog = false;
    private int dialogRetryCount = 0;
    @Override
    protected void onServiceConnected() {
        // 初始化配置
        config = AppConfig.getInstance(this);
        
        // 从SharedPreferences读取选定的应用列表
        android.content.SharedPreferences prefs = getSharedPreferences(config.getPrefsName(), MODE_PRIVATE);
        int selectedAppCount = prefs.getInt("selected_app_count", 0);
        
        List<String> selectedApps = new ArrayList<>();
        if (selectedAppCount > 0) {
            // 读取选定的应用包名
            for (int i = 0; i < selectedAppCount; i++) {
                String packageName = prefs.getString("selected_app_" + i, "");
                if (!packageName.isEmpty()) {
                    selectedApps.add(packageName);
                }
            }
            Log.d("AutoPrivacyService", "从SharedPreferences读取到 " + selectedApps.size() + " 个选定应用");
            for (String pkg : selectedApps) {
                Log.d("AutoPrivacyService", "- " + pkg);
            }
        } else {
            // 如果没有选定的应用，则默认处理所有支持的应用（向后兼容）
            List<String> installedApps = getInstalledApps();
            List<String> supportedApps = filterSupportedApps(installedApps);
            selectedApps.addAll(supportedApps);
            Log.d("AutoPrivacyService", "没有找到选定应用，使用默认的所有支持应用");
        }
        
        // 只处理选定的应用中实际存在的且有脚本支持的应用
        List<String> appsToProcess = new ArrayList<>();
        for (String packageName : selectedApps) {
            if (isJsonExist(packageName)) {
                appsToProcess.add(packageName);
                Log.d("AutoPrivacyService", "添加处理应用: " + packageName);
            } else {
                Log.d("AutoPrivacyService", "跳过无脚本应用: " + packageName);
            }
        }
        
        appQueue.addAll(appsToProcess);
        processNextApp();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d("AutoPrivacyService", "收到事件: " + AccessibilityEvent.eventTypeToString(event.getEventType())
                + ", 包名: " + event.getPackageName());

        // 处理应用启动
        if (isWaitingForAppLaunch &&
                event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                currentPackage.equals(String.valueOf(event.getPackageName()))) {

            isWaitingForAppLaunch = false;
            Log.d("AutoPrivacyService", "检测到目标应用启动成功");
            handler.postDelayed(() -> loadScriptAndExecute(), config.getAppLaunchWaitTime());
        }

        // 处理页面变化检测
        if (isWaitingForPageChange &&
                event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            Log.d("AutoPrivacyService", "检测到页面内容变化");
            if (checkPageChanged()) {
                Log.d("AutoPrivacyService", "确认页面已更新");
                isWaitingForPageChange = false;
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(this::executeNextStep, config.getDefaultDelay());
            }
        }
        // 新增弹窗检测逻辑
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName().equals(currentPackage)) {
                Log.d("DialogDetect", "检测到窗口变化，可能弹出对话框");
                checkAndHandleDialog();
            }
        }
    }
    // 新增弹窗处理方法
    private void checkAndHandleDialog() {
        if (isHandlingDialog) return;

//        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
//        if (rootNode == null) return;

        AccessibilityNodeInfo confirmNode = findNodeByText("继续关闭");
        if (confirmNode != null) {
            Rect bounds = new Rect();
            confirmNode.getBoundsInScreen(bounds);
            clickCenter(bounds);
            Log.d("DialogAction", "找到确认键: ");
            isHandlingDialog = true;
            handler.postDelayed(() -> isHandlingDialog = false, config.getDialogHandleWaitTime());
//                return;
        } else {
            Log.w("DialogAction", "未找到确认键: " );
        }

//        for (AccessibilityNodeInfo node : confirmNodes) {
//            if (node.isClickable() && "android.widget.Button".equals(node.getClassName())) {
//                Log.d("DialogAction", "找到确认按钮");
//                performClickOnNode(node);
//                isHandlingDialog = true;
//                handler.postDelayed(() -> isHandlingDialog = false, 2000);
//                return;
//            }
//        }
//        Log.w("DialogAction", "未找到确认按钮");
    }

    // 新增辅助方法
    private boolean checkPageChanged() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        // 检查是否有新内容出现在点击区域外
        Rect currentBounds = new Rect();
        root.getBoundsInScreen(currentBounds);
        return !currentBounds.contains(lastClickedBounds);
    }

    private void logCurrentWindow() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            Log.d("WindowInfo", "当前窗口: "
                    + "\n包名: " + root.getPackageName()
                    + "\n类名: " + root.getClassName()
                    + "\n子元素数: " + root.getChildCount());
        } else {
            Log.w("WindowInfo", "无法获取当前窗口信息");
        }
    }
    private void processNextApp() {
        if (appQueue.isEmpty()) {
            Log.i("AutoPrivacyService", "所有应用处理完成");
            return;
        }
        currentPackage = appQueue.poll();
        launchApp(currentPackage);
    }

    // 添加新状态变量
    private boolean isWaitingForPageChange = false;
    private long lastActionTime = 0;
    private Rect lastClickedBounds = new Rect();

    // 修改 executeNextStep 方法
    private void executeNextStep() {
        if (currentScript == null) {
            Log.w("AutoPrivacyService", "当前脚本为空");
            return;
        }

        // 检查超时
        if (isWaitingForPageChange && System.currentTimeMillis() - lastActionTime > config.getPageLoadTimeout()) {
            Log.w("AutoPrivacyService", "页面加载超时，继续执行");
            isWaitingForPageChange = false;
        }

        if (isWaitingForPageChange) {
            Log.d("AutoPrivacyService", "正在等待页面变化...");
            return;
        }

        // 记录当前窗口信息
        logCurrentWindow();

        // 优先处理步骤导航
        if (!currentScript.steps.isEmpty()) {
            handleStep(currentScript.steps.remove(0));
            return;
        }

        // 然后处理开关操作
        if (!currentScript.switches.isEmpty()) {
            handleSwitch(currentScript.switches.remove(0));
            return;
        }

        // 全部完成后处理下一个应用
        closeCurrentApp();
        handler.postDelayed(this::processNextApp, 1000);
    }
    // 增强的 handleStep 方法
    private void handleStep(ActionStep step) {
        Rect bounds = parseBounds(step.bounds);
        if (bounds.isEmpty()) {
            Log.e("AutoPrivacyService", "无效的bounds: " + step.bounds);
            currentScript.steps.add(0, step); // 重新放回队列
            executeNextStep();
            return;
        }

        Log.d("AutoPrivacyService", "准备点击 bounds: " + bounds.toShortString());
        logCurrentWindow();

        // 记录点击信息
        lastClickedBounds.set(bounds);
        lastActionTime = System.currentTimeMillis();
        isWaitingForPageChange = true;

        clickCenter(bounds);

        // 设置超时检查
        handler.postDelayed(() -> {
            if (isWaitingForPageChange) {
                Log.w("AutoPrivacyService", "点击超时未检测到页面变化");
                isWaitingForPageChange = false;
                executeNextStep();
            }
        }, config.getPageLoadTimeout());
    }

//    private void handleSwitch(ActionStep sw) {
//        AccessibilityNodeInfo node = findNodeWithScroll(sw.text,3);
//        if (node != null) {
//            Rect bounds = new Rect();
//            node.getBoundsInScreen(bounds);
//            clickCenter(bounds);
//            Log.d("AutoPrivacyService", "找到开关: " + sw.text);
//        } else {
//            Log.w("AutoPrivacyService", "未找到开关: " + sw.text);
//        }
//
//        handler.postDelayed(this::executeNextStep, sw.turnback ? 1500 : 1000);
//    }
private void handleSwitch(ActionStep sw) {
    AccessibilityNodeInfo node = findNodeWithScroll(sw.text, config.getMaxScrollCount());
    if (node != null) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        // 使用新的点击逻辑：Y坐标为文本中心，X坐标为屏幕宽度的配置比例
        clickSwitch(bounds);
        Log.d("AutoPrivacyService", "找到开关: " + sw.text);
    } else {
        Log.w("AutoPrivacyService", "未找到开关: " + sw.text);
    }

    if (sw.turnback) {
        // 延迟执行返回和下一步，确保点击完成
        handler.postDelayed(() -> {
            Log.d("AutoPrivacyService", "执行返回操作");
            performGlobalAction(GLOBAL_ACTION_BACK);
            handler.postDelayed(this::executeNextStep, config.getTurnbackWaitTime()); // 再等待返回生效
        }, config.getTurnbackWaitTime());
    } else {
        handler.postDelayed(this::executeNextStep, config.getClickWaitTime());
    }
}


    // 核心点击方法
    // 修改点击方法添加结果回调
    private void clickCenter(Rect bounds) {
        if (bounds == null || bounds.isEmpty()) {
            Log.e("clickCenter", "传入的bounds无效: " + bounds);
            return;
        }

        // 计算中心坐标
        int x = (bounds.left + bounds.right) / 2;
        int y = (bounds.top + bounds.bottom) / 2;

//        // 获取屏幕分辨率
//        DisplayMetrics metrics = getResources().getDisplayMetrics();
//        int screenWidth = metrics.widthPixels;
//        int screenHeight = metrics.heightPixels;
        DisplayMetrics realMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

// 真实分辨率，包括虚拟导航栏、状态栏
        display.getRealMetrics(realMetrics);

        int realWidth = realMetrics.widthPixels;
        int realHeight = realMetrics.heightPixels;

        Log.d("RealSize", "真实屏幕尺寸: " + realWidth + " x " + realHeight);
//        if(x<=1&&y<=1)//需要归一化
//        {
//            x*=realWidth;
//            y*=realHeight;
//        }
        Log.d("clickCenter", "点击坐标: (" + x + ", " + y + ")");
        Log.d("clickCenter", "屏幕大小: " + realWidth + " x " + realHeight);




//        // 校验坐标是否在屏幕内
//        if (x < 0 || x > screenWidth || y < 0 || y > screenHeight) {
//            Log.e("clickCenter", "点击坐标超出屏幕范围，点击被取消");
//            return;
//        }

        // 构建 Path（轻微滑动，避免部分 ROM 拒绝纯点击）
        Path path = new Path();
        path.moveTo(x, y);
        path.lineTo(x + 1, y + 1); // 模拟轻微滑动

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 100))
                .build();

        boolean dispatchResult = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d("ClickResult", "点击成功完成: " + bounds.toShortString());
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.e("ClickResult", "点击被取消: " + bounds.toShortString());
                isWaitingForPageChange = false;
                executeNextStep();
            }
        }, null);

        if (!dispatchResult) {
            Log.e("clickCenter", "dispatchGesture 调用失败");
        }
    }
    
    // 专门用于点击开关的方法：使用文本节点的中心Y坐标，X坐标为屏幕宽度的88%
    private void clickSwitch(Rect bounds) {
        if (bounds == null || bounds.isEmpty()) {
            Log.e("clickSwitch", "传入的bounds无效: " + bounds);
            return;
        }

        // 获取屏幕分辨率
        DisplayMetrics realMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        display.getRealMetrics(realMetrics);
        
        int realWidth = realMetrics.widthPixels;
        int realHeight = realMetrics.heightPixels;

        // 计算Y坐标为文本节点的中心
        int y = (bounds.top + bounds.bottom) / 2;
        // 设置X坐标为屏幕宽度的配置比例
        int x = (int) (realWidth * config.getSwitchXRatio());

        Log.d("clickSwitch", "点击坐标: (" + x + ", " + y + ")");
        Log.d("clickSwitch", "屏幕大小: " + realWidth + " x " + realHeight);

        // 校验坐标是否在屏幕内
        if (x < 0 || x > realWidth || y < 0 || y > realHeight) {
            Log.e("clickSwitch", "点击坐标超出屏幕范围，点击被取消");
            return;
        }

        // 构建 Path（轻微滑动，避免部分 ROM 拒绝纯点击）
        Path path = new Path();
        path.moveTo(x, y);
        path.lineTo(x + 1, y + 1); // 模拟轻微滑动

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 100))
                .build();

        boolean dispatchResult = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d("ClickResult", "开关点击成功完成: " + bounds.toShortString());
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.e("ClickResult", "开关点击被取消: " + bounds.toShortString());
                isWaitingForPageChange = false;
                executeNextStep();
            }
        }, null);

        if (!dispatchResult) {
            Log.e("clickSwitch", "dispatchGesture 调用失败");
        }
    }

    // 修改 loadScriptAndExecute 方法
    private void loadScriptAndExecute() {
        try {
            InputStream is = getAssets().open(config.getScriptsPath() + currentPackage + ".json");
            String json = convertStreamToString(is);
            currentScript = ScriptParser.parseScript(json);
            Log.d("ScriptLoad", "成功加载脚本，包含："
                    + currentScript.steps.size() + " 个步骤, "
                    + currentScript.switches.size() + " 个开关");
            executeNextStep();
        } catch (Exception e) {
            Log.e("ScriptLoad", "脚本加载失败: " + e.getMessage());
        }
    }

    // 文本查找方法
    private AccessibilityNodeInfo findNodeByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;

        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo node : nodes) {
            if (node.getText() != null &&
                    node.getText().toString().equalsIgnoreCase(text) &&
                    node.isVisibleToUser()) {
                return node;
            }
        }
        return null;
    }
    private AccessibilityNodeInfo findNodeWithScroll(String text, int maxScroll) {
        for (int i = 0; i < maxScroll; i++) {
            AccessibilityNodeInfo node = findNodeByText(text);
            if (node != null) return node;

            // 执行滚动
            performScroll();
            Log.d("Scrolling", "已滚动 " + (i+1) + " 次");
            SystemClock.sleep(config.getScrollWaitTime()); // 等待内容加载
        }
        return null;
    }

    private Rect parseBounds(String boundsStr) {
        Rect rect = new Rect();
        try {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(metrics);
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;

            // 示例格式: [0.803,0.884][1,0.946]
            String[] pairs = boundsStr.split("\\]\\[");
            if (pairs.length != 2) {
                throw new IllegalArgumentException("Invalid bounds format");
            }

            String leftTopStr = pairs[0].replace("[", "").trim();
            String rightBottomStr = pairs[1].replace("]", "").trim();

            String[] lt = leftTopStr.split(",");
            String[] rb = rightBottomStr.split(",");

            if (lt.length != 2 || rb.length != 2) {
                throw new IllegalArgumentException("Invalid coordinate format");
            }

            float left = Float.parseFloat(lt[0]) * screenWidth;
            float top = Float.parseFloat(lt[1]) * screenHeight;
            float right = Float.parseFloat(rb[0]) * screenWidth;
            float bottom = Float.parseFloat(rb[1]) * screenHeight;

            validateCoordinate(left, 0, screenWidth);
            validateCoordinate(top, 0, screenHeight);
            validateCoordinate(right, left, screenWidth);
            validateCoordinate(bottom, top, screenHeight);

            rect.set((int) left, (int) top, (int) right, (int) bottom);

            Log.d("Normalized",
                    "原始值: " + boundsStr +
                            " => 实际坐标: " + rect.toShortString() +
                            " 屏幕: " + screenWidth + "x" + screenHeight);

        } catch (Exception e) {
            Log.e("BoundsParser", "解析失败: " + boundsStr + ", 错误: " + e.getMessage());
        }
        return rect;
    }


    // 新增坐标校验方法
    private void validateCoordinate(float value, float min, float max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "坐标值 " + value + " 超出范围 [" + min + "," + max + "]"
            );
        }
    }

    private void performScroll() {
        DisplayMetrics realMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        display.getRealMetrics(realMetrics);

        int width = realMetrics.widthPixels;
        int height = realMetrics.heightPixels;

        Path path = new Path();
        path.moveTo(width / 2f, height * config.getScrollStartRatio()); // 从配置的位置开始
        path.lineTo(width / 2f, height * config.getScrollEndRatio()); // 向上滑动到配置的位置

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 500))
                .build();

        dispatchGesture(gesture, null, null);
    }


    // 原有基础方法保持不变
    private void closeCurrentApp() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

        private List<String> getInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> packageNames = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            packageNames.add(app.packageName);
            Log.d("InstalledApps", app.packageName); // 打印所有包名
        }
        return packageNames;
    }

    private List<String> filterSupportedApps(List<String> packages) {
        List<String> supportedPackages = new ArrayList<>();
        for (String pkg : packages) {
            boolean exists = isJsonExist(pkg);
//            Log.d("AutoPrivacyService", "检查应用: " + pkg + ", JSON存在: " + exists);
            if (exists) {
                Log.d("AutoPrivacyService","大麦"+pkg+" ");
                supportedPackages.add(pkg);
            }
        }
        return supportedPackages;
    }
    private boolean isJsonExist(String packageName) {
        try {
            InputStream is = getAssets().open(config.getScriptsPath() + packageName + ".json");
            is.close();
            Log.d("AutoPrivacyService", "找到脚本: " + packageName);
            return true;
        } catch (IOException e) {
//            Log.e("AutoPrivacyService", "脚本不存在或读取失败: " + packageName, e);
            return false;
        }
    }

    private void launchApp(String packageName) {
        currentPackage = packageName;
        isWaitingForAppLaunch = true;

        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

        private static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }



    @Override
    public void onInterrupt() {
        Log.e("AutoPrivacyService", "服务中断");
    }
}