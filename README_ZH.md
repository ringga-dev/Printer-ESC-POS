# 🖨️ KmpPrinter (V2.1 企业级增强版)
**面向专业人士的极致 Kotlin 多平台热敏打印套件。**

**语言:** [Bahasa Indonesia](./README.md) | [English](./README_EN.md) | **简体中文**

![Build Status](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publishgithub.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)

---

KmpPrinter 是一款高性能、工业级的 ESC/POS 热敏打印库，专为 **Android, iOS, JVM (Desktop), 和 Web (WASM/JS)** 的无缝集成而设计。

> [!IMPORTANT]
> **企业版 (V2.1)**: 此版本专门针对高流量 POS 系统进行了加固。它包含 **基于线程锁 (Mutex) 的并发保护** 和 **分块传输控制 (Chunked Flow Control)**，以防止低端热敏打印机出现硬件缓冲区溢出。

---

## 📋 平台连接支持情况

| 平台 | 经典蓝牙 | 低功耗蓝牙 (BLE) | USB (OTG) | 网络 (TCP) |
| :--- | :---: | :---: | :---: | :---: |
| **Android** | ✅ | ✅ | ✅ | ✅ |
| **iOS** | ❌ | ✅ | ❌ | ✅ |
| **JVM/Desktop**| ❌ | ❌ | ✅ | ✅ |
| **Web (WASM)** | ✅ | ✅ | ✅ | ✅ |

---

## 💎 高级行业特性

- **🛡️ 硬件级稳定性 (V2.1)**: 内置 `Mutex` 锁，防止并发打印任务导致的数据损坏。自动 `分块发送`（512 字节分块，20ms 延迟）确保在廉价蓝牙打印机上也能稳定运行。
- **🚀 超快图像引擎**: 优化的抖动算法 (**Floyd-Steinberg & Atkinson**)，采用定点整数运算，不仅内存占用极低，且处理速度极快。
- **🖼️ PDF 和矢量图支持**: 能够将 PDF 或 SVG 直接转换为热敏打印优化的位图数据。
- **🌐 Web 平台支持**: 在 KMP WASM/JS 目标中完全支持 WebBluetooth 和 WebUSB。
- **🎨 视觉预览**: 提供实时的收据预览块，确保应用内显示的预览与最终打印结果完全一致。

👉 **[查看完整功能文档](./DOCS_V2_ZH.md)**

---

## 📦 安装指南 (v1.0.3)

👉 **[详细安装指南](./INSTALLATION_ZH.md)**

### 快速集成 (Gradle KMP)
```kotlin
// commonMain
implementation("io.github.ringga-dev:kmp_printer:1.0.3")
```

---

## 🛠️ 高性能代码示例

使用全新的 **Printer DSL** 构建清晰且易于维护的代码：

```kotlin
val printer = KmpPrinter()
val config = PrinterConfig(name = "MTP-II", connectionType = "BLUETOOTH", address = "00:11...")

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
Developed with ❤️ by **Ringga Dev**
