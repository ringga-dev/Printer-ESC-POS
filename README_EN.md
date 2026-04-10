# 🖨️ NggaPrinter
**The Ultimate Kotlin Multiplatform Thermal Printing Suite.**

**Languages:** [Bahasa Indonesia](./README.md) | **English** | [简体中文](./README_ZH.md)

![Build Status](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publishgithub.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Release](https://img.shields.io/github/v/release/ringga-dev/Printer-ESC-POS?color=orange&logo=github)](https://github.com/ringga-dev/Printer-ESC-POS/releases)

NggaPrinter is a high-performance ESC/POS thermal printing library designed for easy integration across **Android, iOS, and JVM (Desktop)**. Using a unified **Connector Pattern** architecture, you can control various thermal printer brands (Bluetooth, USB, Network) with a single, standardized codebase.

> [!TIP]
> **New to KMP?** See the [KMP Integration Guide](./KMP_GUIDE.md) or find ready-to-use binaries on the [Releases](https://github.com/ringga-dev/Printer-ESC-POS/releases) page.

---

## 📦 Latest Binaries
For those who prefer not to use a dependency manager, you can directly download the `.aar` and `.xcframework` files from our releases page:

👉 **[Download NggaPrinter v1.0.0](https://github.com/ringga-dev/Printer-ESC-POS/releases/latest)**

---

## 📥 Installation

We provide various integration methods to suit your needs, whether it's a KMP project, Native Android, or Native iOS.

👉 **[View Full Installation Guide (INSTALLATION_EN.md)](./INSTALLATION_EN.md)**

*   **Method A**: GitHub Maven Repo (Recommended for KMP)
*   **Method B**: Manual Binary Download (AAR/XCFramework)
*   **Method C**: Local Source Module

---

## 🚀 Minimal Requirements
Before starting, ensure your project meets the following requirements:

*   **Kotlin**: 1.9.20 or newer.
*   **Android**: 
    *   Min SDK: **21** (Lollipop).
    *   Target SDK: **34** (Android 14) recommended.
    *   Permissions: Bluetooth Scan, Connect, Fine Location (for discovery).
*   **iOS**: 
    *   Min iOS: **13.0**.
    *   Requirements: Bluetooth LE (CoreBluetooth) capability.
*   **JVM**: Java 11 or newer.

---

## 🛠️ Quick Start

### 1. Initialization & Discovery
Find available printers (Bluetooth/USB/LAN):

```kotlin
val printer = NggaPrinter()

// Discovery via Flow (Reactive UI)
printer.discovery("BLUETOOTH") { log ->
    println("Status: $log")
}.collect { devices ->
    val myPrinter = devices.first()
}
```

### 2. Building Print Commands (Builder)
Use `ESCPosCommandBuilder` equipped with **Safety Wrap Buffer** (Prevents text from overflowing to new lines).

```kotlin
val config = PrinterConfig(name = "MTP-II", connectionType = "BLUETOOTH", address = "00:11...")

val commands = printer.newCommandBuilder(config)
    .initialize()
    .alignCenter()
    .setBold(true)
    .line("NGGA PRINTER STORE")
    .setBold(false)
    .divider()
    // Automatic Table with Weight system (Column Ratio)
    .tableRow(listOf("Iced Coffee", "2x", "$ 4.00"), listOf(2, 1, 1))
    .tableRow(listOf("Toast", "1x", "$ 1.50"), listOf(2, 1, 1))
    .divider()
    .alignRight()
    .line("TOTAL: $ 5.50")
    .feed(3)
    .cutPaper()
    .build()

// Send to printer
printer.printRaw(config, commands).collect { status ->
    if (status is PrintStatus.Success) println("Success")
}
```

---

## 🌟 Professional Features

### 1. Hardware Calibration (Ruler)
Thermal printers have varying dot widths. NggaPrinter provides a calibration tool:
```kotlin
builder.printRuler() // Prints a dot ruler (0, 50, 100...) on physical paper
```
This helps you determine the most precise `paperWidthDots` for your hardware.

### 2. Image Dithering
Print logos or photos with smoother results using the built-in **Floyd-Steinberg Dithering** algorithm, instead of rough black-and-white conversion.

### 3. Safety Layout Logic
All layout functions (`tableRow`, `segmentedLine`, `centeredText`) automatically include a **1-character Safety Buffer** at the end of the line. This guarantees the printer won't trigger an "Auto-Enter" that breaks your receipt's aesthetics.

---

## 🔒 Permissions Policy

### Android
Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### iOS
Add to `Info.plist`:
```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>This app requires Bluetooth access to scan and connect to thermal printers.</string>
```

---

Developed with ❤️ by **Ringga**
