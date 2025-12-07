# 配置指南

本文档将指导您完成项目的初始配置。

## 快速开始

### 1. 创建配置文件

首次使用前，您需要创建自己的配置文件：

```bash
cd app/src/main/assets/
cp config.properties.example config.properties
```

### 2. 配置说明

配置文件位于 `app/src/main/assets/config.properties`，包含以下主要配置项：

#### 重试和超时配置

- `max_retry`: 最大重试次数（默认：3）
- `max_dialog_retry`: 对话框最大重试次数（默认：3）
- `page_load_timeout`: 页面加载超时时间，单位毫秒（默认：5000）
- `default_delay`: 默认延迟时间，单位毫秒（默认：2000）

#### 界面相关配置

- `splash_delay`: 启动页延迟时间，单位毫秒（默认：2000）

#### 点击和手势配置

- `switch_x_ratio`: 开关X轴点击位置比例，范围0.0-1.0（默认：0.88）
  - 如果点击位置偏左，适当增加该值（如0.90）
  - 如果点击位置偏右，适当减少该值（如0.85）

- `app_launch_wait_time`: 应用启动后等待时间，单位毫秒（默认：1500）
- `click_wait_time`: 点击后等待时间，单位毫秒（默认：1000）
- `turnback_wait_time`: 返回操作等待时间，单位毫秒（默认：1500）
- `dialog_handle_wait_time`: 对话框处理等待时间，单位毫秒（默认：2000）

#### 滚动相关配置

- `scroll_wait_time`: 滚动等待时间，单位毫秒（默认：1500）
- `max_scroll_count`: 最大滚动次数（默认：5）
- `scroll_start_ratio`: 滚动起始位置比例，范围0.0-1.0（默认：0.7）
- `scroll_end_ratio`: 滚动结束位置比例，范围0.0-1.0（默认：0.4）

#### 存储配置

- `prefs_name`: SharedPreferences名称（默认：AutoPrivacyPrefs）
- `scripts_path`: 脚本文件路径，相对于assets目录（默认：scripts/）

## 设备特定调整建议

### 高性能设备

如果您的设备性能较好，可以适当减少等待时间以提高效率：

```properties
page_load_timeout=3000
default_delay=1500
app_launch_wait_time=1000
click_wait_time=800
scroll_wait_time=1000
```

### 低性能设备

如果您的设备性能较弱或应用响应较慢，建议增加等待时间：

```properties
page_load_timeout=8000
default_delay=3000
app_launch_wait_time=2500
click_wait_time=1500
scroll_wait_time=2000
```

### 不同屏幕尺寸

对于不同尺寸的屏幕，可能需要调整点击和滚动位置：

**小屏幕设备**（<5.5英寸）：
```properties
switch_x_ratio=0.90
scroll_start_ratio=0.75
scroll_end_ratio=0.35
```

**大屏幕设备**（>6.5英寸）：
```properties
switch_x_ratio=0.85
scroll_start_ratio=0.65
scroll_end_ratio=0.45
```

## 添加新的应用脚本

如需为新的应用添加隐私设置脚本：

1. 在 `app/src/main/assets/scripts/` 目录下创建新文件，命名为 `{应用包名}.json`
2. 按照以下格式编写脚本：

```json
{
  "steps": [
    {
      "bounds": "[左上X,左上Y][右下X,右下Y]",
      "turnback": false
    }
  ],
  "switches": [
    {
      "text": "设置项名称",
      "turnback": false
    }
  ]
}
```

### 参数说明

- `steps`: 导航步骤，使用屏幕坐标比例（0.0-1.0）
- `switches`: 开关操作，使用文本查找
- `turnback`: 是否在操作后返回上一页

### 获取应用包名

可以使用以下方法获取应用包名：

```bash
# 方法1：使用 adb 查看当前前台应用
adb shell dumpsys window | grep mCurrentFocus

# 方法2：使用 adb 列出所有应用
adb shell pm list packages
```

### 获取坐标比例

1. 在Android Studio中使用Layout Inspector
2. 开启开发者选项中的"指针位置"
3. 使用无障碍服务的日志输出

## 常见问题

### Q: 配置修改后不生效？

A: 请确保：
1. 已重新编译和安装应用
2. 配置文件路径正确（`app/src/main/assets/config.properties`）
3. 配置值格式正确（数字不要加引号）

### Q: 点击位置不准确？

A: 调整 `switch_x_ratio` 参数，建议每次调整 0.02-0.05

### Q: 应用执行太快/太慢？

A: 根据设备性能调整各项等待时间参数

## 技术支持

如遇到问题，请在GitHub Issues中提出，并提供：
- 设备型号和Android版本
- 配置文件内容
- 错误日志（如有）

