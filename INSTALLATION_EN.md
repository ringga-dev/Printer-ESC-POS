# 📥 Installation Guide & KMP Masterclass

Welcome to the **NggaPrinter** deep-dive integration guide. This document is designed to help you integrate this library into your Kotlin Multiplatform (KMP) project with professional-grade standards.

---

## 🟢 Option 1: GitHub Maven Repo (Recommended)
This is the most modern and "cleanest" way to integrate for **Kotlin Multiplatform (KMP)** projects.

### 1. Repository Configuration
Open your root `settings.gradle.kts` file. We highly recommend using `dependencyResolutionManagement` to grant automatic access across all modules.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 🚀 NggaPrinter Professional Distribution Path
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
    }
}
```

### 2. Dependency Declaration
Open the `build.gradle.kts` file of your target module (usually `:shared` or `:composeApp`). Add the library to the `commonMain` sourceSet to make the printer available across Android, iOS, and Desktop simultaneously.

```kotlin
// build.gradle.kts (:shared or :composeApp)
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Use stable version 1.0.0 (No 'v' prefix)
            implementation("io.github.ringga-dev:nggaprinter:1.0.0")
        }
    }
}
```

> [!TIP]
> **Important**: Always use version `1.0.0` in your Gradle code. The `v` character is only used for Git Tag labels on GitHub, not for Maven artifact IDs.

---

## 🛠️ Platform Configuration (Required)

To ensure the printer discovery features work correctly, you must add the following permissions to your main application:

### 🤖 Android (AndroidManifest.xml)
Ensure you handle runtime permissions if targeting Android 12+.
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /> <!-- Required for Bluetooth Discovery -->
<uses-permission android:name="android.permission.INTERNET" /> <!-- For Network Printers -->
```

### 🍎 iOS (Info.plist)
Add a Bluetooth usage description to avoid Apple store rejection:
```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>This app requires Bluetooth access to print transaction receipts.</string>
```

---

## ⚡ Usage in KMP (Shared) Module

If you wish to use NggaPrinter inside your `shared` (Business Logic) module, follow this pattern:

```kotlin
// Inside CommonMain (Shared Kotlin Library)
class ReceiptManager {
    private val printer = NggaPrinter() // Unified platform initialization

    suspend fun printReceipt(address: String) {
        val config = PrinterConfig(
            name = "Thermal Printer",
            connectionType = "BLUETOOTH",
            address = address
        )
        // Implement your printing logic here...
    }
}
```

---

## 🔵 Option 2: Binary Release (Manual)
Use this if you have a pure Native project (Swift-only or Kotlin Android-only).

1.  Visit the [Releases Page](https://github.com/ringga-dev/Printer-ESC-POS/releases).
2.  Download the `.aar` (Android) or `.xcframework.zip` (iOS) file.
3.  **Android**: Place in `libs` folder -> `implementation(files("libs/nggaprinter.aar"))`.
4.  **iOS**: Drag the Framework into Xcode -> **Frameworks, Libraries, and Embedded Content**.

---

## ⚪ Option 3: Local Module (Source Code)
Use this if you need to modify the internal logic of NggaPrinter.

1.  Copy the `/printer` folder into your project.
2.  Register in `settings.gradle.kts`: `include(":printer")`.
3.  Implement locally: `implementation(project(":printer"))`.

---
Developed with ❤️ by **Ringga**
