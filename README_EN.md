# 🖨️ KmpPrinter
**The Ultimate Kotlin Multiplatform Thermal Printing Suite for Professionals.**

**Languages:** [Bahasa Indonesia](./README.md) | **English** | [简体中文](./README_ZH.md)

![Build Status](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publishgithub.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Release](https://img.shields.io/github/v/release/ringga-dev/Printer-ESC-POS?color=orange&logo=github)](https://github.com/ringga-dev/Printer-ESC-POS/releases)

---

KmpPrinter is a high-performance ESC/POS thermal printing library designed for seamless integration across **Android, iOS, and JVM (Desktop)**. Powered by a unified **Connector Pattern** architecture, you can command various thermal printer brands (Bluetooth, USB, Network) using a single, standardized codebase.

> [!IMPORTANT]
> **Production Ready**: This library is fortified with an **Auto-Release CI/CD** pipeline. Every version update is guaranteed stable, with binaries (`.aar`, `.jar`, `.xcframework`) always available on the releases page.

---

## 🚀 Premium Features (Why KmpPrinter?)

| Feature | Description | Status |
| :--- | :--- | :---: |
| **KMP Unified** | One codebase for Android, iOS, and Desktop. | ✅ |
| **Safety Buffer** | Prevents text truncation or unintended line-wraps automatically. | ✅ |
| **Floyd-Dithering** | Print images/logos with smooth gradients (no harsh black-and-white). | ✅ |
| **Auto-Calibration** | Built-in `printRuler()` tool to find precise paper dot-width. | ✅ |
| **Reactive Discovery** | `Kotlin Flow` based printer scanning for responsive UIs. | ✅ |

---

## 📦 Rapid Installation (v1.0.1)

For a highly detailed and professional integration guide, please visit our masterclass:

👉 **[INSTALLATION GUIDE & KMP MASTERCLASS](./INSTALLATION_EN.md)**

### Quick Snippet (Gradle KMP)
1. **Repository Settings**:
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
    }
}
```

2. **Dependency**:
```kotlin
// commonMain
implementation("io.github.ringga-dev:kmp_printer:1.0.1")
```

---

## 🛠️ Quick Usage Snippet

```kotlin
val printer = KmpPrinter()
val config = PrinterConfig(name = "MTP-II", connectionType = "BLUETOOTH", address = "00:11...")

val commands = printer.newCommandBuilder(config)
    .initialize()
    .alignCenter()
    .setBold(true)
    .line("KMP PRINTER STORE")
    .setBold(false)
    .divider()
    .tableRow(listOf("Iced Coffee", "2x", "$ 4.00"), listOf(2, 1, 1))
    .divider()
    .alignRight()
    .line("TOTAL: $ 4.00")
    .feed(3)
    .cutPaper()
    .build()

// Print with Flow-based status tracking
printer.printRaw(config, commands).collect { status ->
    if (status is PrintStatus.Success) println("Successfully printed!")
}
```

---

## 🔒 Permissions Policy

### Android
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### iOS
Add `NSBluetoothAlwaysUsageDescription` to your `Info.plist`.

---

## 📖 Deep Dive
*   [Architecture & KMP Design Guide](./KMP_GUIDE.md)
*   [Code Samples & Receipt Templates](./DOCS_AND_SAMPLE.md)
*   [Contributing & License](./CONTRIBUTING.md)

---
Developed with ❤️ by **Ringga**
