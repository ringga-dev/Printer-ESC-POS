# 🖨️ KmpPrinter (V2.0 Enterprise SDK)
**The Ultimate Kotlin Multiplatform Thermal Printing Suite for Professionals.**

**Languages:** [Bahasa Indonesia](./README.md) | **English** | [简体中文](./README_ZH.md)

![Build Status](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publishgithub.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Release](https://img.shields.io/github/v/release/ringga-dev/Printer-ESC-POS?color=orange&logo=github)](https://github.com/ringga-dev/Printer-ESC-POS/releases)

---

KmpPrinter is a high-performance, industrial-grade ESC/POS thermal printing library designed for seamless integration across **Android, iOS, JVM (Desktop), and Web (WASM/JS)**. 

> [!IMPORTANT]
> **Enterprise Edition (V2.0)**: This version is specifically hardened for high-traffic POS systems. It includes **Real-time Status Monitoring**, **Advanced Image Scaling**, and **iOS Native Network framework** support.

---

## 📋 Platform Connectivity Support

| Platform | Bluetooth | BLE | USB (OTG) | Network (TCP) |
| :--- | :---: | :---: | :---: | :---: |
| **Android** | ✅ | ✅ | ✅ | ✅ |
| **iOS** | ❌ | ✅ | ❌ | ✅ |
| **JVM/Desktop**| ❌ | ❌ | ✅ | ✅ |
| **Web (WASM)** | ✅ | ✅ | ✅ | ✅ |

---

## 💎 Premium Industry Features

- **🛡️ Hardened Stability (V2.0)**: Built-in `Mutex` locking prevents data corruption during concurrent print jobs. Automatic `Chunked Sending` ensures reliability on budget bluetooth printers.
- **🚀 Ultra-Fast Image Engine**: Optimized dithering algorithms (**Floyd-Steinberg & Atkinson**) using integer fixed-point arithmetic for minimal RAM usage and maximum speed.
- **📊 Real-time Monitoring**: Accurately detect printer hardware status (Paper Out, Cover Open, Offline) on Android and iOS.
- **🌐 Web Support**: Full support for WebBluetooth and WebUSB in KMP WASM/JS targets.
- **🎨 Visual Preview**: Real-time receipt preview blocks to show exactly what will be printed.

👉 **[BROWSE FULL FEATURE DOCS](./DOCS_V2_EN.md)**

---

## 📦 Installation (v2.0.0)

👉 **[DETAILED INSTALLATION GUIDE](./INSTALLATION_EN.md)**

### Quick Snippet (Gradle KMP)
```kotlin
// commonMain
implementation("io.github.ringga-dev:kmp_printer:2.0.0")
```

---

## 🛠️ Performance-First Usage

Using the new **Printer DSL** for clean and maintainable code:

```kotlin
val printer = KmpPrinter()
val config = PrinterConfig(name = "MTP-II", connectionType = "BLUETOOTH", address = "00:11...")

// Multi-threaded safe printing with build-in flow control
printer.print(config) {
    initialize()
    alignCenter()
    imageAdvanced(logoBytes, width, height, dithering = "ATKINSON")
    setBold(true)
    line("ENTERPRISE POS SYSTEM")
    setBold(false)
    divider()
    tableRow(listOf("Item A", "1x", "$ 10.00"), listOf(2, 1, 1))
    qrCodeNative("https://github.com/ringga-dev", size = 8, center = true)
    feed(3)
    cut()
}
```

---

## 🔒 Permissions Policy

- **Android**: Requires `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, and `ACCESS_FINE_LOCATION` (for legacy scan).
- **iOS**: Add `NSBluetoothAlwaysUsageDescription` to your `Info.plist`.
- **Web**: Requires user interaction (e.g., button click) to trigger the browser's device picker.

---

Developed with ❤️ by **Ringga**
