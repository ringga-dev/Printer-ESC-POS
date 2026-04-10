# 📥 Installation Guide

Pilih salah satu dari tiga metode di bawah ini untuk mengintegrasikan **NggaPrinter** ke dalam aplikasi Anda.

---

## 🟢 Option 1: GitHub Maven Repo (Recommended)
Metode ini adalah yang paling praktis untuk project **Kotlin Multiplatform (KMP)**. Anda tidak perlu mendownload file manual, cukup tambahkan config Gradle.

### 1. Tambah Repository
Buka `settings.gradle.kts` (atau root `build.gradle.kts`) Anda:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Tambahkan link ini
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
    }
}
```

### 2. Tambah Dependency
Di module `commonMain` project KMP Anda:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.ringga-dev:nggaprinter:1.0.0")
        }
    }
}
```

---

## 🔵 Option 2: Binary Release (Manual)
Jika Anda ingin mendownload file `.aar` atau `.xcframework` secara manual tanpa menggunakan dependency manager.

1.  Buka halaman **[Releases](https://github.com/ringga-dev/Printer-ESC-POS/releases)**.
2.  Download file:
    -   `NggaPrinter.aar` (Untuk Android).
    -   `NggaPrinter.xcframework.zip` (Untuk iOS).
3.  **Android**: Letakkan `.aar` di folder `libs` dan tambahkan `implementation(files("libs/NggaPrinter.aar"))`.
4.  **iOS**: Unzip dan drag folder `.xcframework` ke Xcode project Anda di bagian "Frameworks, Libraries, and Embedded Content".

---

## ⚪ Option 3: Local Module (Source)
Gunakan metode ini jika Anda ingin memodifikasi source code library secara langsung.

1.  Salin folder `/printer` dari repository ini ke direktori root project Anda.
2.  Tambahkan ke `settings.gradle.kts`:
    ```kotlin
    include(":printer")
    ```
3.  Implementasikan di `build.gradle.kts`:
    ```kotlin
    commonMain.dependencies {
        implementation(project(":printer"))
    }
    ```

---

## ❓ Mana yang harus saya pilih?
-   Gunakan **Option 1** jika Anda ingin kemudahan update versi secara otomatis.
-   Gunakan **Option 2** jika project Anda murni Android (Native) atau murni iOS (Native Swift) dan tidak ingin pakai Gradle KMP.
-   Gunakan **Option 3** jika Anda adalah kontributor atau ingin melakukan custom logic pada core library.
