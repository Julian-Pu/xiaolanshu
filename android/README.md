# 小蓝书 Android 客户端（Kotlin + Jetpack Compose）

Android 原生 App，MVVM 架构，单 Activity + Navigation Compose。

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- Retrofit + OkHttp + Gson（网络层，自动携带 JWT）
- Coil（图片加载）
- ViewModel + StateFlow（状态管理）
- SharedPreferences（本地登录态）

## 目录结构

```
android/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/wrapper/            # Gradle Wrapper
└── app/
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/xiaolanshu/app/
        │   ├── MainActivity.kt          # 单 Activity 入口
        │   ├── XiaoLanShuApp.kt         # Application（全局配置）
        │   ├── data/
        │   │   ├── ApiClient.kt         # Retrofit + 拦截器
        │   │   ├── ApiService.kt        # 接口定义
        │   │   ├── Models.kt            # 数据模型
        │   │   └── AuthStore.kt         # 登录态存储
        │   ├── ui/
        │   │   ├── theme/               # 蓝色主题
        │   │   ├── navigation/AppNav.kt # 路由
        │   │   ├── components/          # 通用组件（FeedCard 等）
        │   │   └── screens/             # 页面
        │   └── util/TimeUtil.kt
        └── res/                          # 资源与启动图标
```

## 运行方式

1. 用 **Android Studio**（Ladybug 或更新）打开本目录，等待 Gradle Sync。
2. 确保已安装 Android SDK Platform 35+ 与 Build Tools 35+。
3. 启动后端（`server`，端口 3000），App 请求的后端地址在 `app/build.gradle.kts` 的 `API_BASE_URL` 配置。
4. 点击 Run 安装到模拟器/真机；或命令行构建：`.\gradlew.bat assembleDebug`（APK 输出到 `app/build/outputs/apk/debug/`）。

### 后端地址切换（三种场景）

| 场景 | 地址 | 怎么配 |
|---|---|---|
| 本地电脑做服务器 + 模拟器 | `http://10.0.2.2:3000/` | `gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:3000/` |
| 本地电脑做服务器 + 真机（同一 WiFi） | `http://电脑局域网IP:3000/`（默认已配置，如 192.168.2.6） | 无需配置；换网络时改 IP |
| 云端服务器 | `https://你的域名/` 或 `http://公网IP:3000/` | `-PapiBaseUrl=https://你的域名/` |

- 优先级：命令行 `-PapiBaseUrl=` > `gradle.properties` 中 `apiBaseUrl=` > 默认值（局域网 IP）。
- 开发期 App 允许明文 HTTP（`AndroidManifest.xml` 中 `usesCleartextTraffic="true"`）；云端上 HTTPS 后建议移除该项或改用网络安全配置。
- 本地服务器防火墙需放行 3000 端口（Windows：控制面板 → Windows Defender 防火墙 → 入站规则）。

> 说明：本仓库未内置 Android SDK；源码在 Android Studio 中打开即可构建运行。
> 演示账号：13800000001~5 / 123456
> 云端部署完整步骤见 `docs/07-云端部署指南.md`。
