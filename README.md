# 🖨️ KmpPrinter (V2.1.1)
**Solusi Cetak Thermal Kotlin Multiplatform Terbaik untuk Profesional.**

**Bahasa:** **Bahasa Indonesia** | [English](./README_EN.md) | [简体中文](./README_ZH.md)

![Build Status](https://github.com/ringga-dev/kmp-printer/actions/workflows/publishgithub.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)

---

KmpPrinter adalah library pencetakan thermal ESC/POS kelas industri dengan performa tinggi yang dirancang untuk integrasi mulus di **Android, iOS, JVM (Desktop), dan Web (WASM/JS)**.

---

## 📋 Dukungan Konektivitas Platform

| Platform | Bluetooth | BLE | USB (OTG) | Network (TCP) |
| :--- | :---: | :---: | :---: | :---: |
| **Android** | ✅ | ✅ | ✅ | ✅ |
| **iOS** | ❌ | ✅ | ❌ | ✅ |
| **JVM/Desktop**| ❌ | ❌ | ✅ | ✅ |
| **Web (WASM)** | ✅ | ✅ | ✅ | ✅ |

---

## 💎 Fitur Unggulan Industri

- **🛡️ Stabilitas Hardened (V2.1)**: Mekanisme `Mutex` internal mencegah kerusakan data saat ada banyak proses cetak bersamaan.
- **🚀 Ultra-Fast Image Engine**: Algoritma dithering yang dioptimalkan (**Floyd-Steinberg & Atkinson**) menggunakan aritmatika *integer fixed-point*.
- **📊 Real-time Monitoring**: Deteksi status hardware printer secara akurat (Paper Out, Cover Open, Offline) di Android dan iOS.
- **🌐 Web Support**: Dukungan penuh untuk WebBluetooth dan WebUSB pada target KMP WASM/JS.

---

## 📦 Instalasi (v2.1.1)

### 1. Tambahkan Repository GitHub Maven
Tambahkan di `settings.gradle.kts` proyek Anda:
```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/kmp-printer/maven-repo") }
    }
}
```

### 2. Tambahkan Dependency
Tambahkan di modul Anda (contoh: `commonMain` untuk proyek KMP):
```kotlin
dependencies {
    implementation("io.github.ringga-dev:kmp_printer:2.1.1")
}
```

---

## 🛠️ Penggunaan Cepat

Menggunakan **Printer DSL** untuk kode yang bersih:

```kotlin
val printer = KmpPrinter()
val config = PrinterConfig(name = "MTP-II", connectionType = "BLUETOOTH", address = "00:11...")

printer.print(config) {
    initialize()
    alignCenter()
    imageAdvanced(logoBytes, width, height, dithering = "ATKINSON")
    setBold(true)
    line("SISTEM POS ENTERPRISE")
    setBold(false)
    divider()
    tableRow(listOf("Menu A", "1x", "Rp 10.000"), listOf(2, 1, 1))
    qrCodeNative("https://github.com/ringga-dev", size = 8, center = true)
    feed(3)
    cut()
}
```

---

## 📦 Rilis Manual (AAR & XCFramework)
Anda bisa men-download file biner langsung dari [GitHub Releases](https://github.com/ringga-dev/kmp-printer/releases):
- `printer-release.aar` (Android)
- `KmpPrinter.xcframework.zip` (iOS)
- `sonatype-bundle-2.1.1.zip` (Untuk upload manual ke Maven Central)

---

## 🔒 Kebijakan Izin (Permissions)

### Android
Pastikan Anda meminta izin `BLUETOOTH_SCAN` dan `BLUETOOTH_CONNECT` secara runtime pada Android 12+.

---

Developed with ❤️ by **Ringga**