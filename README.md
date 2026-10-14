# 🦀 Clawd 桌宠

一只住在手机里的像素小螃蟹。Android 悬浮窗桌面宠物应用。

## 功能

- **像素螃蟹 SVG 动画** — 待机眨眼、呼吸律动
- **单击互动** — 随机触发 10 种动作（眨眼、开心蹦跳、举爱心、害羞、说话气泡、挠痒、转圈、挥手、比耶、生气等）
- **拖拽移动** — 长按拖拽到屏幕任意位置
- **感知系统** — 充电闪电、低电量红光
- **时段感知**
  - 20:00 自动洗澡（浴帘动画）
  - 12:30 自动吃饭
  - 深夜自动入睡（点击可能吵醒）
- **孤独反应** — 5 分钟无互动会主动求关注
- **开机自启** — 重启手机后自动出现

## 安装

前往 [Releases](../../releases) 页面下载最新 APK，安装后授予悬浮窗权限即可。

## 构建

```bash
# 需要 JDK 17
chmod +x gradlew
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 技术栈

- Android Foreground Service + WebView 悬浮窗
- 纯 SVG + CSS 动画（无图片资源）
- Kotlin
- GitHub Actions 自动构建 + Release
