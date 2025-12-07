# PriSee衍生项目 - 安卓隐私管理助手

<div align="center">

[![Android](https://img.shields.io/badge/Android-24%2B-brightgreen.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com)

一个基于Android无障碍服务的自动化隐私设置助手，帮助您快速批量管理多个应用的隐私设置。

[功能特性](#功能特性) • [快速开始](#快速开始) • [使用指南](#使用指南) • [配置说明](#配置说明) • [贡献指南](#贡献指南)

</div>

---

## 项目简介

在现代移动应用中，隐私设置往往分散在各个应用的深层菜单中，手动逐一配置费时费力。Android Privacy Assistant 通过Android无障碍服务，自动化执行隐私设置操作，让您只需轻点几下就能完成所有应用的隐私配置。

### 主要功能

-  **批量处理**：一次选择多个应用，自动依次处理
-  **脚本驱动**：基于JSON脚本配置，易于扩展新应用
-  **智能识别**：自动识别已安装且支持的应用
-  **灵活配置**：提供丰富的配置项，适配不同设备
-  **开源免费**：MIT许可证，完全开源

### 当前支持的应用

- 大麦 (cn.damai)
- DeepSeek (com.deepseek.chat)
- 广联达图纸大师 (com.glodon.drawingexplorer)
- 抖音直播 (com.ss.android.ugc.live)
- 小红书 (com.xingin.xhs)

> 您可以通过添加JSON脚本文件轻松支持更多应用。

---

## 快速开始

### 前置要求

- Android 7.0 (API 24) 或更高版本
- Android Studio 2022.1 或更高版本（用于构建）
- Java 11 或更高版本

### 安装步骤

1. **克隆仓库**

```bash
git clone https://github.com/YOUR_USERNAME/android-privacy-assistant.git
cd android-privacy-assistant
```

2. **创建配置文件**

```bash
cd app/src/main/assets/
cp config.properties.example config.properties
```

3. **配置参数**（可选）

编辑 `config.properties` 文件，根据您的设备调整参数。详见[配置说明](#配置说明)。

4. **构建项目**

使用Android Studio打开项目，或使用命令行：

```bash
./gradlew assembleDebug
```

5. **安装应用**

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

6. **启用无障碍服务**

- 打开应用
- 根据提示前往系统设置
- 找到"隐私助手服务"并启用

---

##使用指南

### 基本使用流程

1. **启动应用**：打开Android Privacy Assistant
2. **选择应用**：勾选您想要配置隐私设置的应用
   - 绿色标记的应用表示已支持
   - 可使用"全选"或"取消全选"按钮快速选择
3. **开始处理**：点击"开始处理"按钮
4. **启用服务**：首次使用需在系统设置中启用无障碍服务
5. **自动执行**：应用将自动依次打开选中的应用并执行隐私设置

### 注意事项

⚠️ **重要提示**：
- 使用前请确保目标应用已安装并完成初始设置
- 处理过程中请勿操作手机，以免干扰自动化流程
- 首次使用建议选择1-2个应用进行测试
- 不同设备和应用版本可能需要调整配置参数

---

##配置说明

### 配置文件位置

`app/src/main/assets/config.properties`

### 主要配置项

#### 时间相关配置（单位：毫秒）

| 配置项 | 说明 | 默认值 | 建议范围 |
|-------|------|--------|---------|
| `page_load_timeout` | 页面加载超时时间 | 5000 | 3000-10000 |
| `default_delay` | 默认操作延迟 | 2000 | 1000-3000 |
| `app_launch_wait_time` | 应用启动等待时间 | 1500 | 1000-3000 |
| `click_wait_time` | 点击后等待时间 | 1000 | 500-2000 |
| `scroll_wait_time` | 滚动等待时间 | 1500 | 1000-3000 |

#### 手势相关配置

| 配置项 | 说明 | 默认值 | 建议范围 |
|-------|------|--------|---------|
| `switch_x_ratio` | 开关X轴点击位置比例 | 0.88 | 0.80-0.95 |
| `scroll_start_ratio` | 滚动起始位置比例 | 0.7 | 0.6-0.8 |
| `scroll_end_ratio` | 滚动结束位置比例 | 0.4 | 0.3-0.5 |

#### 设备特定优化建议

**高性能设备**（旗舰机型）：
```properties
page_load_timeout=3000
default_delay=1500
app_launch_wait_time=1000
```

**低性能设备**（入门机型）：
```properties
page_load_timeout=8000
default_delay=3000
app_launch_wait_time=2500
```

详细配置说明请参考 [SETUP.md](SETUP.md)。

---

##添加新应用支持

### 步骤说明

1. **获取应用包名**

```bash
# 查看当前前台应用
adb shell dumpsys window | grep mCurrentFocus
```

2. **创建脚本文件**

在 `app/src/main/assets/scripts/` 目录下创建 `{包名}.json` 文件。

3. **编写脚本**

```json
{
  "steps": [
    {
      "bounds": "[0.1,0.2][0.9,0.3]",
      "turnback": false
    }
  ],
  "switches": [
    {
      "text": "隐私设置",
      "turnback": false
    },
    {
      "text": "个性化推荐",
      "turnback": true
    }
  ]
}
```

### 脚本说明

- **steps**: 导航步骤，使用归一化坐标（0.0-1.0）
  - `bounds`: 点击区域，格式为 `[左上X,左上Y][右下X,右下Y]`
  - `turnback`: 点击后是否返回上一页
  
- **switches**: 开关操作，基于文本查找
  - `text`: 要查找的文本内容
  - `turnback`: 操作后是否返回上一页

### 获取坐标方法

1. **使用开发者选项**：
   - 开启"指针位置"
   - 记录点击位置的实际坐标
   - 除以屏幕宽/高得到归一化坐标

2. **使用Layout Inspector**：
   - Android Studio → Tools → Layout Inspector
   - 查看元素的bounds属性

3. **查看日志**：
   - 应用运行时会输出详细的坐标日志
   - 使用 `adb logcat | grep AutoPrivacy` 查看

---

##项目结构

```
android-privacy-assistant/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   ├── config.properties          # 用户配置文件（不提交）
│   │   │   │   ├── config.properties.example  # 配置文件模板
│   │   │   │   └── scripts/                   # 应用脚本目录
│   │   │   │       ├── cn.damai.json
│   │   │   │       ├── com.xingin.xhs.json
│   │   │   │       └── ...
│   │   │   ├── java/com/example/demo_323/
│   │   │   │   ├── MainActivity.java          # 主界面
│   │   │   │   ├── AutoPrivacyService.java    # 无障碍服务核心
│   │   │   │   ├── AppConfig.java             # 配置管理类
│   │   │   │   ├── ScriptParser.java          # 脚本解析器
│   │   │   │   └── ...
│   │   │   └── res/                           # 资源文件
│   └── build.gradle.kts
├── .gitignore
├── README.md                                   # 项目说明文档
├── SETUP.md                                    # 配置指南
└── ...
```
