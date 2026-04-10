# 🖨️ NggaPrinter
**The Ultimate Kotlin Multiplatform Thermal Printing Suite for Professionals.**

**Languages:** **Bahasa Indonesia** | [English](./README_EN.md) | [简体中文](./README_ZH.md)

![Build Status](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publishgithub.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Release](https://img.shields.io/github/v/release/ringga-dev/Printer-ESC-POS?color=orange&logo=github)](https://github.com/ringga-dev/Printer-ESC-POS/releases)

---

NggaPrinter adalah library thermal printing ESC/POS yang dirancang untuk performa tinggi dan kemudahan integrasi di **Android, iOS, dan JVM (Desktop)**. Menggunakan arsitektur **Connector Pattern** yang terpadu, Anda dapat mengontrol berbagai merk printer thermal (Bluetooth, USB, Network) dengan satu standar kode yang sama.

> [!IMPORTANT]
> **Production Ready**: Library ini sudah dilengkapi dengan sistem **Auto-Release CI/CD**. Setiap update versi dijamin stabil dan binari (`.aar`, `.jar`, `.xcframework`) selalu tersedia di halaman rilis.

---

## 🚀 Fitur Unggulan (Why NggaPrinter?)

| Fitur | Penjelasan | Status |
| :--- | :--- | :---: |
| **KMP Unified** | Satu kode untuk Android, iOS, dan Desktop. | ✅ |
| **Safety Buffer** | Mencegah teks terpotong atau "tumpah" ke baris baru secara tidak sengaja. | ✅ |
| **Floyd-Dithering** | Cetak gambar/logo dengan gradasi halus (bukan hitam-putih kasar). | ✅ |
| **Auto-Calibration** | Alat bantu `printRuler()` untuk mencari lebar kertas (dots) yang presisi. | ✅ |
| **Reactive Discovery** | Scan printer berbasis `Kotlin Flow` untuk UI yang responsif. | ✅ |

---

## 📦 Pemasangan Cepat (v1.0.0)

Untuk integrasi yang paling detail dan profesional, silakan lihat panduan lengkap kami:

👉 **[PANDUAN INSTALASI & KMP MASTERCLASS](./INSTALLATION.md)**

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
implementation("io.github.ringga-dev:nggaprinter:1.0.0")
```

---

## 🛠️ Contoh Kode Cepat

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
    .tableRow(listOf("Iced Coffee", "2x", "Rp 40.000"), listOf(2, 1, 1))
    .divider()
    .alignRight()
    .line("TOTAL: Rp 40.000")
    .feed(3)
    .cutPaper()
    .build()

// Kirim ke printer dengan Flow status
printer.printRaw(config, commands).collect { status ->
    if (status is PrintStatus.Success) println("Berhasil dicetak!")
}
```

---

## 🔒 Kebijakan Izin (Permissions)

### Android
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### iOS
Tambahkan `NSBluetoothAlwaysUsageDescription` ke `Info.plist` Anda.

---

## 📖 Dokumentasi Lanjut
*   [Panduan Arsitektur & KMP Guide](./KMP_GUIDE.md)
*   [Contoh Kode & Sampel Struk](./DOCS_AND_SAMPLE.md)
*   [Kontribusi & Lisensi](./CONTRIBUTING.md)

---
Developed with ❤️ by **Ringga**