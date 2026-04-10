# 🖨️ NggaPrinter
**The Ultimate Kotlin Multiplatform Thermal Printing Suite.**

![Build Status](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publish.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ringga-dev/nggaprinter.svg)](https://central.sonatype.com/)

NggaPrinter adalah library thermal printing ESC/POS yang dirancang untuk performa tinggi dan kemudahan integrasi di **Android, iOS, dan JVM (Desktop)**. Menggunakan arsitektur **Connector Pattern** yang terpadu, Anda dapat mengontrol berbagai merk printer thermal (Bluetooth, USB, Network) dengan satu standar kode yang sama.

> [!TIP]
> **Baru menggunakan KMP?** Lihat [Panduan Integrasi KMP](file:///d:/Android/project/Printer%20ESCPOS/KMP_GUIDE.md) untuk detail setup tiap platform.

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

### Method A: Maven Central / JitPack (Recommended)
Add this to your `build.gradle.kts` inside the `commonMain` source set:

```kotlin
// In your root settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
        mavenCentral()
    }
}

// In your shared/module build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.ringga-dev:Printer-ESC-POS:1.0.0")
        }
    }
}
```

### Method B: Local Module
1.  Salin folder `/printer` ke direktori root project Anda.
2.  Tambahkan modul ke `settings.gradle.kts`:
    ```kotlin
    include(":printer")
    ```
3.  Implementasikan di `build.gradle.kts` modul Anda:
    ```kotlin
    commonMain.dependencies {
        implementation(project(":printer"))
    }
    ```

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