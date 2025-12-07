package com.example.demo_323;
import com.google.gson.Gson;
public class ScriptParser {
    private static final Gson gson = new Gson(); // 创建静态实例

    public static PrivacyScript parseScript(String json) {
        return gson.fromJson(json, PrivacyScript.class); // 现在可以正确访问
    }
}