plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.xiaolanshu.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xiaolanshu.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "1.9.0"

        // ===== 后端地址配置 =====
        // 默认：本机局域网 IP（本地电脑做服务器，真机与电脑连同一 WiFi 直连，如 192.168.2.6）
        // 切换方式（任选其一，无需改源码）：
        //   1) 命令行：gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:3000/（模拟器访问宿主机）
        //   2) gradle.properties 中加一行：apiBaseUrl=http://你的地址/
        //   3) Android Studio：Settings → Build → Gradle → 添加 -PapiBaseUrl=...
        // 云端部署：-PapiBaseUrl=https://你的域名/ 或 -PapiBaseUrl=http://公网IP:3000/
        val apiBaseUrl = (project.findProperty("apiBaseUrl") as String?) ?: "http://192.168.2.6:3000/"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 签名：命令行传入 -PkeystorePath=xxx.jks -PkeystorePassword=... -PkeyAlias=... -PkeyPassword=... 即用正式签名；
            // 未提供 keystore 时回退 debug 签名，保证 assembleRelease 产物也能直接安装（仅限内部测试/首发体验）。
            signingConfig = if (project.hasProperty("keystorePath")) {
                signingConfigs.create("release") {
                    storeFile = file(project.property("keystorePath") as String)
                    storePassword = (project.findProperty("keystorePassword") ?: "") as String
                    keyAlias = (project.findProperty("keyAlias") ?: "") as String
                    keyPassword = (project.findProperty("keyPassword") ?: "") as String
                }
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // 网络
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 图片
    implementation("io.coil-kt:coil-compose:2.7.0")

    // 视频播放（Media3 ExoPlayer）
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
}
