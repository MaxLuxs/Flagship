plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    kotlin("native.cocoapods")
    // Publishing: uncomment when ready
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
        summary = "Flagship LaunchDarkly Provider"
        homepage = "https://github.com/maxluxs/Flagship"
        version = "0.1.1"
        ios.deploymentTarget = "14.0"
        
        pod("LaunchDarkly") {
            version = "8.0.0"
        }
        
        framework {
            baseName = "FlagshipProviderLaunchDarkly"
            isStatic = true
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "FlagshipProviderLaunchDarkly"
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
            // LaunchDarkly Android SDK
            implementation("com.launchdarkly:launchdarkly-android-client-sdk:5.1.1")
        }
        
        iosMain.dependencies {
            // iOS uses REST fallback or can use native SDK via cocoapods
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "io.maxluxs.flagship.provider.launchdarkly"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

