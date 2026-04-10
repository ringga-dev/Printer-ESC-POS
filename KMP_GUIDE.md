# 📚 NggaPrinter: KMP Integration Guide

Panduan ini akan membantu Anda mengintegrasikan **NggaPrinter** ke dalam project Kotlin Multiplatform (KMP) Anda secara profesional.

---

## 1. Setup Repository & Dependency

### Tambahkan Repository
Buka `settings.gradle.kts` (Level Project) dan tambahkan repository GitHub Maven:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Repository NggaPrinter di GitHub
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
    }
}
```

### Tambahkan Dependency
Tambahkan library ke dalam blok `commonMain` pada file `build.gradle.kts` di modul shared Anda:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.ringga-dev:nggaprinter:1.0.0")
        }
    }
}
```

> [!TIP]
> Untuk opsi instalasi manual (Binari .aar/.xcframework), silakan baca [Panduan Instalasi Lengkap](./INSTALLATION.md).

---

## 2. Platform Setup (Wajib)

### 🤖 Android
Tambahkan izin Bluetooth dan Lokasi di `composeApp/src/androidMain/AndroidManifest.xml`:

```xml
<!-- Izin Bluetooth untuk Android 12+ -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Izin Lokasi (diperlukan untuk scanning Bluetooth pada versi Android lama dan Android 12+) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 🍎 iOS
Tambahkan deskripsi penggunaan Bluetooth di `iosApp/Info.plist`:

```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>Aplikasi ini memerlukan akses Bluetooth untuk mencetak struk ke printer thermal.</string>
<key>NSBluetoothPeripheralUsageDescription</key>
<string>Aplikasi ini memerlukan akses Bluetooth untuk mencetak struk ke printer thermal.</string>
```

---

## 3. Implementasi Kode di CommonMain

Gunakan `NggaPrinter` sebagai entry point utama. Anda bisa menggunakan Dependency Injection (Koin) atau Singleton manual.

### Inisialisasi
```kotlin
val printer = NggaPrinter() // Automatis mendeteksi platform
```

### Discovery (Cari Printer)
Kami menggunakan `Flow` untuk memberikan hasil secara real-time:

```kotlin
val config = DiscoveryConfig(showVirtualDevices = false)

printer.discovery("BLUETOOTH", config) { log ->
    println(log) // Pantau proses di sini
}.collect { devices ->
    // 'devices' adalah List<DiscoveredPrinter>
    devices.forEach { device ->
        println("Ditemukan: ${device.name} - ${device.address}")
    }
}
```

### Membuat Struk & Mencetak
Gunakan `newCommandBuilder` untuk menyusun layout secara deklaratif.

```kotlin
val printerConfig = PrinterConfig(
    name = "MPT-II",
    connectionType = "BLUETOOTH",
    address = "MAC_ADDRESS_PRINTER", // Diambil dari hasil discovery
    characterPerLine = 31 // Standard untuk 58mm
)

val printJob = printer.newCommandBuilder(printerConfig)
    .initialize()
    .alignCenter()
    .setBold(true)
    .line("TOKO KELONTONG")
    .setBold(false)
    .divider()
    .tableRow(listOf("Kopi", "1x", "15.000"), listOf(2, 1, 1))
    .tableRow(listOf("Gula", "2x", "24.000"), listOf(2, 1, 1))
    .divider()
    .alignRight()
    .line("TOTAL: 39.000")
    .feed(3)
    .build()

printer.printRaw(printerConfig, printJob).collect { status ->
    when (status) {
        is PrintStatus.Connecting -> println("Sedang menyambung...")
        is PrintStatus.Printing -> println("Sedang mencetak...")
        is PrintStatus.Success -> println("Berhasil!")
        is PrintStatus.Error -> println("Error: ${status.message}")
    }
}
```

---

## 🔧 Fitur Lanjut

### Kalibrasi Kertas (Print Ruler)
Jika teks Anda terlihat terpotong atau terlalu lebar, gunakan fitur kalibrasi untuk mencari tahu jumlah `dots` maksimal printer Anda:

```kotlin
val rulerJob = builder.printRuler()
printer.printRaw(config, rulerJob).collect { /* ... */ }
```

### Image Printing (Dithering)
Anda bisa mencetak gambar dari `ByteArray` (Android Bitmap atau iOS UIImage ke ByteArray):

```kotlin
builder.image(imageByteArray, maxWidth = 384) // 384 dots adalah standar 58mm
```

---

## ⚠️ Troubleshooting
- **Android**: Pastikan pengguna sudah memberikan izin lokasi dan Bluetooth secara run-time.
- **iOS**: Pastikan printer thermal Anda mendukung **Bluetooth Low Energy (BLE)**. NggaPrinter menggunakan BLE untuk konektivitas iOS yang lebih stabil.
- **Datarows**: Jika tabel tidak lurus, periksa nilai `characterPerLine` (CPL). Standar 58mm adalah 31-32, standar 80mm adalah 42-48.
