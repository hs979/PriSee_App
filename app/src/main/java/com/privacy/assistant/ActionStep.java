// ActionStep.java
package com.privacy.assistant;

import android.graphics.Rect;

public class ActionStep {
    public String bounds;       // 改为存储坐标范围
    public boolean turnback = false;

    // 新增字段用于开关操作
    public String text;       // 开关文本

    public ActionStep() {}
}

// 新增 PrivacyScript.java
