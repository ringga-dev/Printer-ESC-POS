# 📥 安装指南

选择以下三种方法之一，将 **NggaPrinter** 集成到您的应用程序中。

---

## 🟢 选项 1: GitHub Maven 仓库 (推荐)
这是 **Kotlin 多平台 (KMP)** 项目最实用的方法。无需手动下载文件，只需添加 Gradle 配置即可。

### 1. 添加仓库
打开您的 `settings.gradle.kts` (或根目录 `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // 添加此链接
        maven { url = uri("https://raw.githubusercontent.com/ringga-dev/Printer-ESC-POS/maven-repo") }
    }
}
```

### 2. 添加依赖
在您项目的 `commonMain` 模块依赖中:

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

## 🔵 选项 2: 二进制发布 (手动)
如果您更喜欢手动下载 `.aar` 或 `.xcframework` 文件，而不使用依赖管理器。

1.  访问 **[发布 (Releases)](https://github.com/ringga-dev/Printer-ESC-POS/releases)** 页面。
2.  下载文件:
    -   `NggaPrinter.aar` (适用于 Android)。
    -   `NggaPrinter.xcframework.zip` (适用于 iOS)。
3.  **Android**: 将 `.aar` 放入 `libs` 文件夹，并添加 `implementation(files("libs/NggaPrinter.aar"))`。
4.  **iOS**: 解压并将 `.xcframework` 文件夹拖入 Xcode 项目的 "Frameworks, Libraries, and Embedded Content" 部分。

---

## ⚪ 选项 3: 本地模块 (源码)
如果您希望直接修改库的源代码，请使用此方法。

1.  将此仓库中的 `/printer` 文件夹复制到您项目的根目录。
2.  将其添加到 `settings.gradle.kts`:
    ```kotlin
    include(":printer")
    ```
3.  在您的 `build.gradle.kts` 中实现:
    ```kotlin
    commonMain.dependencies {
        implementation(project(":printer"))
    }
    ```

---

## ❓ 我该选择哪一个？
-   如果您希望轻松实现自动版本更新，请选择 **选项 1**。
-   如果您的项目是纯 Android (原生) 或纯 iOS (原生 Swift)，且不想使用 KMP Gradle，请选择 **选项 2**。
-   如果您是贡献者或想在核心库中实现自定义逻辑，请选择 **选项 3**。
