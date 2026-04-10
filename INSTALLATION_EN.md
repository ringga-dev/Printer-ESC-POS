# 📥 Installation Guide

Choose one of the following three methods to integrate **NggaPrinter** into your application.

---

## 🟢 Option 1: GitHub Maven Repo (Recommended)
This method is the most practical for **Kotlin Multiplatform (KMP)** projects. No manual file downloads required; just add the Gradle config.

### 1. Add Repository
Open your `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Add this link
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
    }
}
```

### 2. Add Dependency
In your project's `commonMain` module dependencies:

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
If you prefer to download `.aar` or `.xcframework` files manually without using a dependency manager.

1.  Go to the **[Releases](https://github.com/ringga-dev/Printer-ESC-POS/releases)** page.
2.  Download the files:
    -   `NggaPrinter.aar` (For Android).
    -   `NggaPrinter.xcframework.zip` (For iOS).
3.  **Android**: Place the `.aar` in the `libs` folder and add `implementation(files("libs/NggaPrinter.aar"))`.
4.  **iOS**: Unzip and drag the `.xcframework` folder into your Xcode project under "Frameworks, Libraries, and Embedded Content".

---

## ⚪ Option 3: Local Module (Source)
Use this method if you wish to modify the library source code directly.

1.  Copy the `/printer` folder from this repository to your project's root directory.
2.  Add it to `settings.gradle.kts`:
    ```kotlin
    include(":printer")
    ```
3.  Implement it in your `build.gradle.kts`:
    ```kotlin
    commonMain.dependencies {
        implementation(project(":printer"))
    }
    ```

---

## ❓ Which one should I choose?
-   Choose **Option 1** if you want easy automatic version updates.
-   Choose **Option 2** if your project is pure Android (Native) or pure iOS (Native Swift) and you don't want to use KMP Gradle.
-   Choose **Option 3** if you are a contributor or want to implement custom logic in the core library.
