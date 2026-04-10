# 🖨️ NggaPrinter
**终极 Kotlin 多平台热敏打印套件。**

**语言:** [Bahasa Indonesia](./README.md) | [English](./README_EN.md) | **简体中文**

![构建状态](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publishgithub.yml/badge.svg)
[![许可证](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![版本](https://img.shields.io/github/v/release/ringga-dev/Printer-ESC-POS?color=orange&logo=github)](https://github.com/ringga-dev/Printer-ESC-POS/releases)

**语言:** [Bahasa Indonesia](./README.md) | [English](./README_EN.md) | **简体中文**

NggaPrinter 是一款高性能的 ESC/POS 热敏打印库，旨在简化 **Android、iOS 和 JVM (Desktop)** 平台的集成。通过统一的 **连接器模式 (Connector Pattern)** 架构，您可以使用一套标准化的代码控制各种品牌的热敏打印机（蓝牙、USB、网络）。

> [!TIP]
> **初次接触 KMP？** 请参阅 [KMP 集成指南](./KMP_GUIDE.md) 或在 [发布 (Releases)](https://github.com/ringga-dev/Printer-ESC-POS/releases) 页面查找即插即用的二进制文件。

---

## 📦 最新二进制文件
如果您不想使用依赖管理器，可以直接从我们的发布页面下载 `.aar` 和 `.xcframework` 文件：

👉 **[下载 NggaPrinter v1.0.0](https://github.com/ringga-dev/Printer-ESC-POS/releases/latest)**

---

## 📥 安装

我们提供多种集成方式以满足您的需求，无论是 KMP 项目、原生 Android 还是原生 iOS。

👉 **[查看完整安装指南 (INSTALLATION_ZH.md)](./INSTALLATION_ZH.md)**

*   **方法 A**: GitHub Maven 仓库（KMP 项目推荐）
*   **方法 B**: 手动二进制下载 (AAR/XCFramework)
*   **方法 C**: 本地源码模块

---

## 🚀 最低配置要求
在开始之前，请确保您的项目满足以下要求：

*   **Kotlin**: 1.9.20 或更高版本。
*   **Android**: 
    *   最低 SDK: **21** (Lollipop)。
    *   建议目标 SDK: **34** (Android 14)。
    *   权限: 蓝牙扫描、连接、精确位置（用于设备发现）。
*   **iOS**: 
    *   最低 iOS 版本: **13.0**。
    *   硬件要求: 需支持低功耗蓝牙 (BLE/CoreBluetooth)。
*   **JVM**: Java 11 或更高版本。

---

## 🛠️ 快速上手

### 1. 初始化与设备发现
查找可用的打印机（蓝牙/USB/局域网）：

```kotlin
val printer = NggaPrinter()

// 通过 Flow 进行发现 (响应式 UI)
printer.discovery("BLUETOOTH") { log ->
    println("状态: $log")
}.collect { devices ->
    val myPrinter = devices.first()
}
```

### 2. 构建打印指令 (Builder)
使用配备 **安全换行缓冲 (Safety Wrap Buffer)** 的 `ESCPosCommandBuilder`（防止文本溢出到新行）。

```kotlin
val config = PrinterConfig(name = "MTP-II", connectionType = "BLUETOOTH", address = "00:11...")

val commands = printer.newCommandBuilder(config)
    .initialize()
    .alignCenter()
    .setBold(true)
    .line("NGGA 打印机商店")
    .setBold(false)
    .divider()
    // 自动表格系统 (使用权重/列比例)
    .tableRow(listOf("冰咖啡", "2x", "¥ 28.00"), listOf(2, 1, 1))
    .tableRow(listOf("吐司", "1x", "¥ 12.00"), listOf(2, 1, 1))
    .divider()
    .alignRight()
    .line("总计: ¥ 40.00")
    .feed(3)
    .cutPaper()
    .build()

// 发送到打印机
printer.printRaw(config, commands).collect { status ->
    if (status is PrintStatus.Success) println("打印成功")
}
```

---

## 🌟 专业特性

### 1. 硬件校准 (Ruler)
热敏打印机的点宽各不相同。NggaPrinter 提供了一个校准工具：
```kotlin
builder.printRuler() // 在物理纸张上打印刻度尺 (0, 50, 100...)
```
这有助于您为具体的硬件确定最精确的 `paperWidthDots`（纸张点宽）。

### 2. 图像抖动 (Dithering)
使用内置的 **Floyd-Steinberg 抖动算法** 打印徽标或照片，效果比粗糙的黑白转换更平滑。

### 3. 安全布局逻辑
所有布局函数（`tableRow`、`segmentedLine`、`centeredText`）在行尾都会自动包含 **1 个字符的安全缓冲**。这保证了打印机不会触发破坏收据美感的“自动回车”。

---

## 🔒 权限策略

### Android
在 `AndroidManifest.xml` 中添加：
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### iOS
在 `Info.plist` 中添加：
```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>此应用需要蓝牙权限以扫描并连接热敏打印机。</string>
```

---

由 **Ringga** 精心打造 ❤️
