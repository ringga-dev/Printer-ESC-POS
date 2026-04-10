import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
    id("signing")
    // Dokka for professional Javadoc required by Maven Central
    id("org.jetbrains.dokka") version "1.9.20"
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

    val xcf = XCFramework("NggaPrinter")
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "NggaPrinter"
            isStatic = true
            freeCompilerArgs += listOf("-Xbinary=bundleId=io.github.ringga_dev.nggaprinter")
            xcf.add(this)
        }
    }

    jvm()
    
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

// Requirements for Maven Central: Javadoc Jar (using Dokka)
val kmpJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    try {
        from(tasks.named("dokkaHtml"))
    } catch (e: Exception) {
        // Fallback for when Dokka fails due to internal metadata bugs
        // Allows the build to continue while we wait for a Dokka fix
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.property("LIB_GROUP").toString()
            artifactId = "nggaprinter"
            version = project.property("LIB_VERSION").toString()

            artifact(kmpSourcesJar)
            artifact(kmpJavadocJar)

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

    repositories {
        maven {
            name = "Sonatype"
            // Using the modern S01 endpoint which is recommended for new Central Portal accounts
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername")
                password = System.getenv("ORG_GRADLE_PROJECT_mavenCentralPassword")
            }
        }
        maven {
            name = "LocalRepo"
            url = uri("${rootProject.projectDir}/build/repo")
        }
    }
}

signing {
    val signingKey = System.getenv("ORG_GRADLE_PROJECT_signingKey")
    val signingPassword = System.getenv("ORG_GRADLE_PROJECT_signingPassword")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}
