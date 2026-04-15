# 📥 Panduan Instalasi & KMP Masterclass

Selamat datang di panduan integrasi mendalam **KmpPrinter**. Dokumentasi ini dirancang agar Anda bisa mengintegrasikan library ini ke proyek Kotlin Multiplatform (KMP) Anda dengan standar profesional.

---

## 📋 Prasyarat & Batasan (Requirements & Limitations)

Sebelum memulai integrasi, pastikan lingkungan pengembangan Anda memenuhi kriteria berikut:

| Platform | Versi Minimal | Keterangan |
| :--- | :--- | :--- |
| **Kotlin** | 2.3.20+ | Mendukung Compose Multiplatform terbaru. |
| **Android** | SDK 24 (7.0) | Mendukung Bluetooth & Network Socket. |
| **iOS** | 13.0+ | Architecture arm64 (Real devices & Simulators). |
| **JVM/Desktop** | Java 11+ | Digunakan untuk Serial/Bluetooth Desktop. |
| **Gradle** | 8.0+ | Dibutuhkan untuk plugin KMP terbaru. |

### ⚠️ Batasan Penting:
1.  **ESC/POS Only**: Library ini dirancang khusus untuk printer thermal yang mengikuti standar perintah ESC/POS.
2.  **No USB on iOS**: Karena kebijakan Apple, koneksi USB langsung ke printer thermal tidak tersedia. Gunakan Bluetooth atau Network.
3.  **Image DPI**: Pencetakan gambar dilakukan via bit-image raster. Untuk hasil terbaik, gunakan gambar dengan kontras tinggi (Dithering sudah otomatis dilakukan oleh library).

---

## 🟢 Opsi 1: GitHub Maven Repo (Direkomendasikan)
Metode ini adalah cara paling modern dan "bersih" untuk proyek **Kotlin Multiplatform (KMP)**.

### 1. Konfigurasi Repository
Buka file `settings.gradle.kts` (Root Project) Anda. Sangat direkomendasikan menggunakan `dependencyResolutionManagement` agar semua modul mendapatkan akses otomatis.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 🚀 Jalur Distribusi Profesional KmpPrinter
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
    }
}
```

### 2. Deklarasi Dependency
Buka file `build.gradle.kts` di modul target Anda (biasanya `:composeApp` atau `:shared`). Tambahkan library di bagian `commonMain` agar printer tersedia di Android, iOS, dan Desktop sekaligus.

```kotlin
// build.gradle.kts (:shared atau :composeApp)
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Gunakan versi stabil 1.0.3 (Tanpa awalan 'v')
            implementation("io.github.ringga-dev:kmp_printer:1.0.3")
        }
    }
}
```

> [!TIP]
> **Penting**: Selalu gunakan versi `1.0.3` di kode Gradle Anda. Huruf `v` hanya digunakan untuk label Tag di GitHub, bukan untuk ID artifact Maven.

---

## 🛠️ Konfigurasi Platform (Wajib)

Agar fitur pencarian printer (Discovery) berjalan lancar, Anda harus menambahkan izin berikut pada aplikasi utama Anda.

### 🤖 Android (AndroidManifest.xml)
Pastikan Anda menangani izin runtime jika Anda mentargetkan Android 12+.
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /> <!-- Dibutuhkan untuk Bluetooth Discovery -->
<uses-permission android:name="android.permission.INTERNET" /> <!-- Untuk Network Printer -->
```

### 🍎 iOS (Info.plist)
Tambahkan deskripsi penggunaan Bluetooth agar Apple tidak menolak aplikasi Anda:
```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>Aplikasi ini membutuhkan akses Bluetooth untuk mencetak struk belanja.</string>
```

---

## ⚡ Cara Panggil di Modul KMP (Shared)

Jika Anda ingin menggunakan KmpPrinter di dalam modul `shared` (Business Logic), ikuti pola ini:

```kotlin
// Di dalam CommonMain (Kotlin Library)
class ReceiptManager {
    private val printer = KmpPrinter() // Inisialisasi otomatis lintas platform

    suspend fun printReceipt(address: String) {
        val config = PrinterConfig(
            name = "Thermal Printer",
            connectionType = "BLUETOOTH",
            address = address
        )
        // Lakukan logika pencetakan di sini...
    }
}
```

---

## 🔵 Opsi 2: Binari Manual (AAR/XCFramework)
Gunakan jika Anda memiliki project Native murni (Swift Only atau Kotlin Android Only).

1.  Buka [Halaman Releases](https://github.com/ringga-dev/Printer-ESC-POS/releases).
2.  Ambil file `.aar` (Android) atau `.xcframework.zip` (iOS).
3.  **Android**: Taruh di folder `libs` -> `implementation(files("libs/kmp_printer.aar"))`.
4.  **iOS**: Masukkan Framework ke dalam Xcode -> **Frameworks, Libraries, and Embedded Content**.

---

---

## ⚠️ Hal-hal yang Sering Menjadi Kendala (Common hazards)

### 1. Masalah Resolusi Maven
Jika Gradle mengatakan "Could not find io.github.ringga-dev:kmp_printer", pastikan:
-   Anda menggunakan URL `https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo`.
-   Anda menambahkan blok repository di `settings.gradle.kts` bagian `dependencyResolutionManagement` bukannya di file build modul.

### 2. Duplikasi Artifact
Jangan mencapur **Opsi 1** (Maven) dengan **Opsi 3** (Local Project). Ini akan menyebabkan error `Duplicate Classes` saat kompilasi. Pilih salah satu.

### 3. Masalah Kotlin Version Match
Library ini di-build dengan Kotlin **2.3.20**. Jika project Anda menggunakan versi yang jauh lebih rendah, Anda mungkin menemui error `Metadata version mismatch`. Disarankan untuk mengupdate Kotlin project Anda ke versi terbaru.

---

## ⚪ Opsi 3: Local Module (Source Code)
Gunakan jika Anda ingin memodifikasi logika internal KmpPrinter.

1.  Salin folder `/printer` ke project Anda.
2.  Daftarkan di `settings.gradle.kts`: `include(":printer")`.
3.  Panggil lokal: `implementation(project(":printer"))`.

---
Developed with ❤️ by **Ringga**
