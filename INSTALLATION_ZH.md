# 📥 安装指南与 KMP 专家课程

欢迎查看 **NggaPrinter** 的深度集成指南。 本文档旨在帮助您以专业级标准将此库集成到您的 Kotlin 多平台 (KMP) 项目中。

---

## 🟢 选项 1: GitHub Maven 仓库 (推荐)
这是 **Kotlin 多平台 (KMP)** 项目最现代、最“干净”的集成方式。

### 1. 仓库配置
打开项目根目录下的 `settings.gradle.kts` 文件。 我们强烈建议使用 `dependencyResolutionManagement` 来确保所有模块都能自动访问。

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 🚀 NggaPrinter 专业分发路径
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
    }
}
```

### 2. 依赖声明
打开目标模块（通常是 `:shared` 或 `:composeApp`）的 `build.gradle.kts` 文件。 将库添加到 `commonMain` 源集中，使打印机同时支持 Android、iOS 和桌面端。

```kotlin
// build.gradle.kts (:shared 或 :composeApp)
kotlin {
    sourceSets {
        commonMain.dependencies {
            // 使用稳定版本 1.0.0 (不带 'v' 前缀)
            implementation("io.github.ringga-dev:nggaprinter:1.0.0")
        }
    }
}
```

> [!TIP]
> **重要**: 在 Gradle 代码中请务必使用版本号 `1.0.0`。 字符 `v` 仅用于 GitHub 上的 Git Tag 标签，不用于 Maven 制品 ID。

---

## 🛠️ 平台配置 (必填)

为了确保打印机扫描（Discovery）功能正常运行，您必须在主应用程序中添加以下权限：

### 🤖 Android (AndroidManifest.xml)
如果目标版本为 Android 12+，请确保处理了运行时权限。
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /> <!-- 蓝牙扫描所需权限 -->
<uses-permission android:name="android.permission.INTERNET" /> <!-- 针对网络打印机 -->
```

### 🍎 iOS (Info.plist)
添加蓝牙使用说明以避免被 Apple Store 拒绝：
```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>此应用程序需要蓝牙权限才能打印交易收据。</string>
```

---

## ⚡ 在 KMP (Shared) 模块中使用

如果您希望在 `shared`（业务逻辑）模块中使用 NggaPrinter，请参考以下模式：

```kotlin
// 在 CommonMain (共享 Kotlin 库) 中
class ReceiptManager {
    private val printer = NggaPrinter() // 统一的多平台初始化

    suspend fun printReceipt(address: String) {
        val config = PrinterConfig(
            name = "Thermal Printer",
            connectionType = "BLUETOOTH",
            address = address
        )
        // 在此处实现您的打印逻辑...
    }
}
```

---

## 🔵 选项 2: 二进制发布 (手动)
如果您使用的是纯原生项目（仅 Swift 或仅 Kotlin Android），请使用此选项。

1.  访问 [发布页面](https://github.com/ringga-dev/Printer-ESC-POS/releases)。
2.  下载 `.aar` (Android) 或 `.xcframework.zip` (iOS) 文件。
3.  **Android**: 存放在 `libs` 文件夹中 -> `implementation(files("libs/nggaprinter.aar"))`。
4.  **iOS**: 将 Framework 拖入 Xcode -> **Frameworks, Libraries, and Embedded Content**。

---

## ⚪ 选项 3: 本地模块 (源代码)
如果您需要修改 NggaPrinter 的内部逻辑，请使用此选项。

1.  将 `/printer` 文件夹复制到您的项目中。
2.  在 `settings.gradle.kts` 中注册: `include(":printer")`。
3.  本地实现: `implementation(project(":printer"))`。

---
Developed with ❤️ by **Ringga**
