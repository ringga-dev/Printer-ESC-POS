plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
}

group = "io.github.ringga-dev"
version = "1.0.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "NggaPrinter"
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.ringga-dev"
            artifactId = "nggaprinter"
            version = "1.0.0"
            
            pom {
                name.set("NggaPrinter")
                description.set("Professional Kotlin Multiplatform Thermal Printing Library for ESC/POS.")
                url.set("https://github.com/ringga-dev/Printer-ESC-POS")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("ringga")
                        name.set("Ringga")
                        email.set("ringga@dev.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/ringga-dev/Printer-ESC-POS.git")
                    developerConnection.set("scm:git:ssh://github.com/ringga-dev/Printer-ESC-POS.git")
                    url.set("https://github.com/ringga-dev/Printer-ESC-POS")
                }
            }
        }
    }
}
