# 🖨️ KmpPrinter
**The Ultimate Kotlin Multiplatform Thermal Printing Suite for Professionals.**

**Languages:** **Bahasa Indonesia** | [English](./README_EN.md) | [简体中文](./README_ZH.md)

![Build Status](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publishgithub.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Release](https://img.shields.io/github/v/release/ringga-dev/Printer-ESC-POS?color=orange&logo=github)](https://github.com/ringga-dev/Printer-ESC-POS/releases)

---

KmpPrinter adalah library thermal printing ESC/POS yang dirancang untuk performa tinggi dan kemudahan integrasi di **Android, iOS, dan JVM (Desktop)**. Menggunakan arsitektur **Connector Pattern** yang terpadu, Anda dapat mengontrol berbagai merk printer thermal (Bluetooth, USB, Network) dengan satu standar kode yang sama.

> [!IMPORTANT]
> **Production Ready**: Library ini sudah dilengkapi dengan sistem **Auto-Release CI/CD**. Setiap update versi dijamin stabil dan binari (`.aar`, `.jar`, `.xcframework`) selalu tersedia di halaman rilis.

---

## 📋 Spesifikasi & Batasan (Requirement & Limitations)

### Spesifikasi Minimal
Agar library dapat berjalan dengan optimal, pastikan proyek Anda memenuhi syarat berikut:
- **Kotlin**: v2.3.20 atau lebih tinggi.
- **Android**: API Level 24+ (Android 7.0 Nougat).
- **iOS**: iOS 13.0+ (Architecture arm64).
- **JVM/Desktop**: Java 11 atau lebih tinggi.
- **Gradle**: v8.0 atau lebih tinggi.

### Batasan Library (Known Limitations)
- **Protokol**: Hanya mendukung perintah standar **ESC/POS**.
- **Konektivitas iOS**: Hanya mendukung Bluetooth (BLE/Classic tergantung hardware). Koneksi USB pada iOS tidak didukung karena batasan sistem operasi.
- **Pencetakan Gambar**: Menggunakan mode **Raster Bit Image** (Mode paling kompatibel, namun ukuran data bisa besar untuk gambar resolusi tinggi).
- **Encoding**: Default menggunakan UTF-8. Karakter khusus di luar standar ASCII bergantung pada dukungan Code Page di firmware printer Anda.

---

## 🚀 Fitur Unggulan (Why KmpPrinter?)

| Fitur | Penjelasan | Status |
| :--- | :--- | :---: |
| **KMP Unified** | Satu kode untuk Android, iOS, dan Desktop. | ✅ |
| **Native Barcode/QR** | Cetak QR & Barcode tajam via hardware (v2.0). | ✅ |
| **USB OTG** | Dukungan kabel USB di Android (v2.0). | ✅ |
| **Visual Preview** | Lihat struk di layar sebelum diprint (v2.0). | ✅ |
| **Auto-Discovery** | Scan otomatis printer di jaringan/IP. | ✅ |
| **Floyd-Dithering** | Cetak gambar dengan gradasi halus. | ✅ |

---

## 💎 KmpPrinter V2.0 (New!)
Versi terbaru kini hadir dengan fitur-fitur "Expert" untuk implementasi kelas industri.

👉 **[LIHAT DOKUMENTASI FITUR V2.0](./DOCS_V2.md)**

---

---

## 📦 Pemasangan Cepat (v1.0.3)

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
implementation("io.github.ringga-dev:kmp_printer:1.0.3")
```

---

## 🛠️ Contoh Kode Cepat

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

## 🔍 Troubleshooting & FAQ

### 1. Unresolved Reference: `KmpPrinter`
Pastikan Anda sudah menambahkan `mavenCentral()` dan URL GitHub Maven di `settings.gradle.kts`. Jika menggunakan Gradle versi lama, tambahkan di `build.gradle` root.
```kotlin
maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
```

### 2. Error Saat Build Release (Android)
Library ini sudah menyertakan **Consumer ProGuard Rules**. Anda tidak perlu menambahkan konfigurasi `-keep` manual untuk class internal library. Pastikan `minifyEnabled true` tetap aktif jika Anda ingin optimasi.

### 3. Masalah Izin Bluetooth di Android 12+
Pastikan Anda meminta izin `BLUETOOTH_SCAN` dan `BLUETOOTH_CONNECT` secara runtime. Gunakan `PrinterPermissionManager` (tersedia di library) untuk mempermudah pengecekan.

---

## 📖 Dokumentasi Lanjut
*   [Panduan Arsitektur & KMP Guide](./KMP_GUIDE.md)
*   [Contoh Kode & Sampel Struk](./DOCS_AND_SAMPLE.md)
*   [Kontribusi & Lisensi](./CONTRIBUTING.md)

---
Developed with ❤️ by **Ringga**