# 🖨️ KmpPrinter (V3.0 Enterprise SDK)
**Solusi Cetak Thermal Kotlin Multiplatform Terbaik untuk Profesional.**

**Bahasa:** **Bahasa Indonesia** | [English](./README_EN.md) | [简体中文](./README_ZH.md)

![Build Status](https://github.com/ringga-dev/Printer-ESC-POS/actions/workflows/publishgithub.yml/badge.svg)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)

---

KmpPrinter adalah library pencetakan thermal ESC/POS kelas industri dengan performa tinggi yang dirancang untuk integrasi mulus di **Android, iOS, JVM (Desktop), dan Web (WASM/JS)**.

> [!IMPORTANT]
> **Enterprise Edition (V3.0)**: Versi ini telah diperkuat (*hardened*) khusus untuk sistem POS dengan traffic tinggi. Dilengkapi dengan **Real-time Status Monitoring**, **Advanced Image Scaling**, dan **iOS Native Network framework** support.

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

- **🛡️ Stabilitas Hardened (V2.0)**: Mekanisme `Mutex` internal mencegah kerusakan data saat ada banyak proses cetak bersamaan. Fitur `Chunked Sending` otomatis memastikan keandalan pada printer bluetooth murah.
- **🚀 Ultra-Fast Image Engine**: Algoritma dithering yang dioptimalkan (**Floyd-Steinberg & Atkinson**) menggunakan aritmatika *integer fixed-point* untuk penggunaan RAM minimal dan kecepatan maksimal.
- **📊 Real-time Monitoring**: Deteksi status hardware printer secara akurat (Paper Out, Cover Open, Offline) di Android dan iOS.
- **🌐 Web Support**: Dukungan penuh untuk WebBluetooth dan WebUSB pada target KMP WASM/JS.
- **🎨 Visual Preview**: Blok pratinjau struk secara real-time untuk menunjukkan hasil cetak secara presisi di aplikasi.

👉 **[BACA DOKUMENTASI FITUR LENGKAP](./DOCS_V2.md)**

---

## 📦 Instalasi (v3.0.0)

👉 **[PANDUAN INSTALASI DETAIL](./INSTALLATION.md)**

### Cuplikan Cepat (Gradle KMP)
```kotlin
// commonMain
implementation("io.github.ringga-dev:kmp_printer:3.0.0")
```

---

## 🛠️ Penggunaan Performa-Tinggi

Menggunakan **Printer DSL** baru untuk kode yang bersih dan mudah dipelihara:

```kotlin
val printer = KmpPrinter()
val config = PrinterConfig(name = "MTP-II", connectionType = "BLUETOOTH", address = "00:11...")

// Pencetakan aman multi-thread dengan flow control bawaan
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

## 🔒 Kebijakan Izin (Permissions)

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