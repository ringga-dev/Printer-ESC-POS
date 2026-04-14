# 📚 KmpPrinter: Pro KMP Integration Guide

Panduan ini akan membantu Anda mengintegrasikan **KmpPrinter** ke dalam project Kotlin Multiplatform (KMP) Anda dengan standar industri yang stabil dan terukur.

---

## 🏗️ 1. Arsitektur: 100% Shared Logic
KmpPrinter menggunakan arsitektur **Connector-Bridge Pattern**. Artinya, hampir 99% logika pencetakan Anda (layout, tabel, gambar) berada di modul `commonMain`. 

Anda tidak perlu menulis kode Swift di Xcode atau Java di modul Android secara terpisah untuk mencetak struk yang sama.

---

## 🚀 2. Setup Repository & Dependency

### Tambahkan Repository (Settings.gradle.kts)
Untuk performa build yang lebih cepat, gunakan `dependencyResolutionManagement`:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // 🚀 Jalur Distribusi Resmi KmpPrinter
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
    }
}
```

### Tambahkan Dependency (Build.gradle.kts)
Tambahkan library ke dalam blok `commonMain` agar otomatis tersedia di semua platform (Android, iOS, JVM):

```kotlin
// build.gradle.kts (:shared atau :composeApp)
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Selalu gunakan versi rilisan stabil 1.0.1
            implementation("io.github.ringga-dev:kmp_printer:1.0.1")
        }
    }
}
```

---

## 🛠️ 3. Platform Configuration (Wajib)

### 🤖 Android
Tambahkan izin di `AndroidManifest.xml`. Pastikan Anda menangani izin lokasi dan bluetooth secara run-time jika menargetkan Android 12+.

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /> <!-- Required for Discovery -->
<uses-permission android:name="android.permission.INTERNET" /> <!-- For LAN printers -->
```

### 🍎 iOS
Tambahkan deskripsi penggunaan Bluetooth di `iosApp/Info.plist` agar aplikasi Anda tidak ditolak oleh App Store:

```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>This app requires Bluetooth access to print transaction receipts.</string>
```

---

## ⚡ 4. Implementasi Kode di Modul Shared

### Inisialisasi Singleton
```kotlin
// Inisialisasi otomatis mendeteksi platform (Android, iOS, atau JVM)
val printer = KmpPrinter() 
```

### Discovery Berbasis Flow
Kami menggunakan **Kotlin Flow** agar UI Anda (Compose atau SwiftUI) bisa menangkap perubahan status printer secara reaktif:

```kotlin
printer.discovery("BLUETOOTH") { log ->
    println("Status Scanning: $log") 
}.collect { devices ->
    // 'devices' adalah List<DiscoveredPrinter>
    val myPrinter = devices.firstOrNull()
}
```

### Mencetak dengan CommandBuilder
Gunakan sistem unit `weight` untuk membuat tabel yang rapi di berbagai lebar kertas:

```kotlin
val printerConfig = PrinterConfig(
    name = "MPT-II",
    connectionType = "BLUETOOTH",
    address = "MAC_ADDRESS_PRINTER", // Dari hasil discovery
    characterPerLine = 32 // Standar 58mm
)

val printJob = printer.newCommandBuilder(printerConfig)
    .initialize()
    .alignCenter()
    .line("STRUK PEMBAYARAN")
    .divider()
    // Kolom 1: Nama (Weight 2), Kolom 2: Qty (Weight 1), Kolom 3: Harga (Weight 1)
    .tableRow(listOf("Kopi", "1x", "15.000"), listOf(2, 1, 1))
    .tableRow(listOf("Gula", "2x", "24.000"), listOf(2, 1, 1))
    .divider()
    .alignRight()
    .line("TOTAL: 39.000")
    .feed(3)
    .build()

// Kirim ke printer
printer.printRaw(printerConfig, printJob).collect { status ->
    when (status) {
        is PrintStatus.Success -> println("Selesai Cetak!")
        is PrintStatus.Error -> println("Gagal: ${status.message}")
        else -> println("Status: $status")
    }
}
```

---

## 🛡️ 5. Jaminan Kualitas (CI/CD Pipeline)
Library ini dikelola dengan standar DevOps profesional:
*   **Auto-Release**: Setiap versi divalidasi dan dibangun secara otomatis oleh GitHub Actions.
*   **No-Stop Guarantee**: Sistem rilis ke GitHub terjamin tetap jalan meskipun ada gangguan pada server Maven eksternal.
*   **Verified Binaries**: File `.aar` dan `.xcframework` selalu tersedia di halaman rilis setiap kali ada update kode.

---
Developed with ❤️ by **Ringga**
