# 🖨️ NggaPrinter
**The Ultimate Kotlin Multiplatform Thermal Printing Suite.**

**Languages:** **Bahasa Indonesia** | [English](./README_EN.md) | [简体中文](./README_ZH.md)

![Build Status](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publishgithub.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Release](https://img.shields.io/github/v/release/ringga-dev/Printer-ESC-POS?color=orange&logo=github)](https://github.com/ringga-dev/Printer-ESC-POS/releases)

NggaPrinter adalah library thermal printing ESC/POS yang dirancang untuk performa tinggi dan kemudahan integrasi di **Android, iOS, dan JVM (Desktop)**. Menggunakan arsitektur **Connector Pattern** yang terpadu, Anda dapat mengontrol berbagai merk printer thermal (Bluetooth, USB, Network) dengan satu standar kode yang sama.

> [!TIP]
> **Baru menggunakan KMP?** Lihat [Panduan Integrasi KMP](./KMP_GUIDE.md) atau cari binari siap pakai di halaman [Releases](https://github.com/ringga-dev/Printer-ESC-POS/releases).

---

## 📦 Latest Binaries
Untuk Anda yang tidak ingin menggunakan dependency manager, Anda bisa langsung mendownload file `.aar` dan `.xcframework` dari halaman release kami:

👉 **[Download NggaPrinter v1.0.0](https://github.com/ringga-dev/Printer-ESC-POS/releases/latest)**

---

## 🚀 Minimal Requirements
Sebelum memulai, pastikan project Anda memenuhi persyaratan minimum berikut:

*   **Kotlin**: 1.9.20 atau yang lebih baru.
*   **Android**: 
    *   Min SDK: **21** (Lollipop).
    *   Target SDK: **34** (Android 14) direkomendasikan.
    *   Izin: Bluetooth Scan, Connect, Fine Location (untuk discovery).
*   **iOS**: 
    *   Min iOS: **13.0**.
    *   Persyaratan: Bluetooth LE (CoreBluetooth) capability.
*   **JVM**: Java 11 atau yang lebih baru.

---

## 📥 Installation

Kami menyediakan berbagai metode integrasi untuk memudahkan Anda, baik itu project KMP, Android Native, maupun iOS Native.

👉 **[Lihat Panduan Instalasi Lengkap (INSTALLATION.md)](./INSTALLATION.md)**

*   **Method A**: GitHub Maven Repo (Recommended for KMP)
*   **Method B**: Manual Binary Download (AAR/XCFramework)
*   **Method C**: Local Source Module

---

## 🛠️ Quick Start

### 1. Inisialisasi & Discovery
Cari printer yang tersedia di sekitar (Bluetooth/USB/LAN):

```kotlin
val printer = NggaPrinter()

// Discovery via Flow (Reactive UI)
printer.discovery("BLUETOOTH") { log ->
    println("Status: $log")
}.collect { devices ->
    val myPrinter = devices.first()
}
```

### 2. Membangun Perintah Cetak (Builder)
Gunakan `ESCPosCommandBuilder` yang sudah dilengkapi dengan **Safety Wrap Buffer** (Mencegah teks tumpah ke baris baru).

```kotlin
val config = PrinterConfig(name = "MTP-II", connectionType = "BLUETOOTH", address = "00:11...")

val commands = printer.newCommandBuilder(config)
    .initialize()
    .alignCenter()
    .setBold(true)
    .line("TOKO MADJU JAYA")
    .setBold(false)
    .divider()
    // Tabel otomatis dengan sistem Weight (Rasio kolom)
    .tableRow(listOf("Kopi Susu", "2x", "Rp 40.000"), listOf(2, 1, 1))
    .tableRow(listOf("Roti Bakar", "1x", "Rp 15.000"), listOf(2, 1, 1))
    .divider()
    .alignRight()
    .line("TOTAL: Rp 55.000")
    .feed(3)
    .cutPaper()
    .build()

// Kirim ke printer
printer.printRaw(config, commands).collect { status ->
    if (status is PrintStatus.Success) println("Berhasil")
}
```

---

## 🌟 Fitur Unggulan Profesional

### 1. Hardware Calibration (Ruler)
Printer thermal memiliki variasi lebar *dots* yang berbeda-beda. NggaPrinter menyediakan alat kalibrasi:
```kotlin
builder.printRuler() // Mencetak penggaris dots (0, 50, 100...) di kertas fisik
```
Hasil cetakan ini membantu Anda menentukan `paperWidthDots` yang paling presisi untuk hardware Anda.

### 2. Image Dithering
Cetak logo atau foto dengan hasil yang lebih halus menggunakan algoritma **Floyd-Steinberg Dithering** bawaan, bukan sekadar hitam-putih kasar.

### 3. Safety Layout Logic
Semua fungsi layout (`tableRow`, `segmentedLine`, `centeredText`) secara otomatis memberikan **Safety Buffer** 1 karakter di akhir baris. Ini menjamin printer tidak akan melakukan "Auto-Enter" yang merusak estetika struk Anda.

---

## 🔒 Permissions Policy

### Android
Tambahkan ke `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### iOS
Tambahkan ke `Info.plist`:
```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>Dibutuhkan untuk mencari dan menghubungkan ke printer bluetooth.</string>
```

---

Developed with ❤️ by **Ringga**