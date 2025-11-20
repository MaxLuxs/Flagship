plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
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

    sourceSets {
        androidMain.dependencies {
            implementation(projects.flagshipCore)
            implementation(libs.androidx.core.ktx)
        }
    }
}

android {
    namespace = "io.maxluxs.flagship.platform.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

