import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

group = project.property("LIB_GROUP").toString()
version = project.property("LIB_VERSION").toString()

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    val xcf = XCFramework("KmpPrinter")
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KmpPrinter"
            isStatic = true
            freeCompilerArgs += listOf("-Xbinary=bundleId=io.github.ringga_dev.kmp_printer")
            xcf.add(this)
        }
    }

    jvm()

    wasmJs {
        browser()
        binaries.executable()
    }

    js(IR) {
        browser()
        binaries.executable()
    }
    
    targets.all {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

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
            implementation("org.apache.pdfbox:pdfbox:3.0.1")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "ngga.ring.printer"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable.add("MissingPermission")
    }
}

// Requirements for Maven Central: Sources Jar
val kmpSourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets.getByName("commonMain").kotlin)
}

// Dummy Javadoc JAR to satisfy Maven Central requirements and avoid Dokka bugs
val kmpJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(projectDir.resolve("README.md"))
}

publishing {
    publications {
        withType<MavenPublication> {
            artifact(kmpSourcesJar)
            artifact(kmpJavadocJar)

            pom {
                name.set("KmpPrinter")
                description.set("KmpPrinter: Professional Kotlin Multiplatform Thermal Printing Library for ESC/POS.")
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
                        email.set("ringgadev@gmail.com")
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

    repositories {
        // 1. Sonatype Central (Official Release)
        maven {
            name = "Sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("MAVEN_USERNAME") ?: (project.findProperty("ossrhUsername") as? String)
                password = System.getenv("MAVEN_PASSWORD") ?: (project.findProperty("ossrhPassword") as? String)
            }
        }
        // 2. Local Repo for GitHub Maven Branch (maven-repo)
        maven {
            name = "LocalRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY") ?: (project.findProperty("signingKey") as? String)
    val signingPassword = System.getenv("GPG_PASSWORD") ?: (project.findProperty("signingPassword") as? String)
    
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
