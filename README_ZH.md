# 🖨️ NggaPrinter
**专为专业人士打造的终极 Kotlin 多平台热敏打印套件。**

**语言:** [Bahasa Indonesia](./README.md) | [English](./README_EN.md) | **简体中文**

![Build Status](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publishgithub.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Release](https://img.shields.io/github/v/release/ringga-dev/Printer-ESC-POS?color=orange&logo=github)](https://github.com/ringga-dev/Printer-ESC-POS/releases)

---

NggaPrinter 是一款高性能的 ESC/POS 热敏打印库，旨在轻松集成到 **Android、iOS 和 JVM (Desktop)**。 凭借统一的 **Connector Pattern** 架构，您可以使用一套标准代码控制各种品牌的热敏打印机（蓝牙、USB、网络）。

> [!IMPORTANT]
> **生产就绪**: 该库通过 **Auto-Release CI/CD** 流水线提供支持。 每个版本的更新都经过稳定性保证，且二进制文件（`.aar`、`.jar`、`.xcframework`）始终可以在发布页面下载。

---

## 🚀 核心特性 (为什么选择 NggaPrinter?)

| 特性 | 说明 | 状态 |
| :--- | :--- | :---: |
| **KMP 统一** | 一套代码支持 Android, iOS, 和 Desktop。 | ✅ |
| **安全缓冲区** | 自动防止文本截断或不必要的自动换行。 | ✅ |
| **Floyd 抖动算法** | 以平滑的渐变色打印图像/Logo（非粗糙黑白）。 | ✅ |
| **自动校准** | 内置 `printRuler()` 工具，寻找精确的打印点宽。 | ✅ |
| **响应式扫描** | 基于 `Kotlin Flow` 的打印机扫描，适用于响应式 UI。 | ✅ |

---

## 📦 快速安装 (v1.0.0)

如需查看最详尽、最专业的集成指南，请访问：

👉 **[安装指南与 KMP 专家课程](./INSTALLATION_ZH.md)**

### 快速代码片段 (Gradle KMP)
1. **仓库设置**:
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
    }
}
```

2. **依赖配置**:
```kotlin
// commonMain
implementation("io.github.ringga-dev:nggaprinter:1.0.0")
```

---

## 🛠️ 快速使用示例

```kotlin
val printer = NggaPrinter()
val config = PrinterConfig(name = "MTP-II", connectionType = "BLUETOOTH", address = "00:11...")

val commands = printer.newCommandBuilder(config)
    .initialize()
    .alignCenter()
    .setBold(true)
    .line("NGGA PRINTER STORE")
    .setBold(false)
    .divider()
    .tableRow(listOf("冰咖啡", "2x", "¥ 40.00"), listOf(2, 1, 1))
    .divider()
    .alignRight()
    .line("合计: ¥ 40.00")
    .feed(3)
    .cutPaper()
    .build()

// 使用基于 Flow 的状态跟踪进行打印
printer.printRaw(config, commands).collect { status ->
    if (status is PrintStatus.Success) println("打印成功！")
}
```

---

## 🔒 权限策略

### Android
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### iOS
请在 `Info.plist` 中添加 `NSBluetoothAlwaysUsageDescription` 描述。

---

## 📖 深度探索
*   [架构与 KMP 设计指南](./KMP_GUIDE.md)
*   [代码示例与小票模板](./DOCS_AND_SAMPLE.md)
*   [贡献与许可](./CONTRIBUTING.md)

---
Developed with ❤️ by **Ringga**
