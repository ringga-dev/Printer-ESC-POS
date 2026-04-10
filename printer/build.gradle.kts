plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Printer"
            isStatic = true
        }
    }
    
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("io.github.aakira:napier:2.7.1")
        }

        androidMain.dependencies {
            implementation("androidx.appcompat:appcompat:1.7.0")
            implementation("com.google.android.material:material:1.12.0")
        }
        
        jvmMain.dependencies {
            implementation("com.fazecast:jSerialComm:2.11.0")
            implementation("net.java.dev.jna:jna:5.14.0")
            implementation("net.java.dev.jna:jna-platform:5.14.0")
        }
    }
}

android {
    namespace = "ngga.ring.printer"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
