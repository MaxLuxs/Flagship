plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    // Skip Dokka due to Firebase dependencies
    // alias(libs.plugins.dokka)
    kotlin("native.cocoapods")
    // Publishing: uncomment when ready to publish
    // `maven-publish`
    // signing
}

// apply(from = rootProject.file("gradle/publish.gradle.kts"))

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                }
            }
        }
    }

    cocoapods {
        summary = "Flagship Firebase Provider"
        homepage = "https://github.com/maxluxs/Flagship"
        version = "0.1.0"
        ios.deploymentTarget = "14.0"
        
        pod("FirebaseRemoteConfig") {
            version = "11.8.0"
        }
        
        framework {
            baseName = "FlagshipProviderFirebase"
            isStatic = true
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "FlagshipProviderFirebase"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.flagshipCore)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.config)
            implementation(libs.kotlinx.coroutines.play.services)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "io.maxluxs.flagship.provider.firebase"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

