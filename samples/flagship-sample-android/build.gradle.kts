import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
    kotlin("android")
    // Flagship codegen plugin (available via mavenLocal)
    id("io.maxluxs.flagship.codegen") version "0.1.1"
}

android {
    namespace = "io.maxluxs.flagship.sampleandroid"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    
    defaultConfig {
        applicationId = "io.maxluxs.flagship.sampleandroid"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    buildFeatures {
        compose = true
    }
}

// Flagship codegen configuration
flagshipCodegen {
    configFile = file("../flagship-sample/flags.json")
    outputDir = file("build/generated/flagship")
    packageName = "io.maxluxs.flagship.generated"
}

// Add generated sources to source sets
android {
    sourceSets {
        getByName("main") {
            java.srcDirs("build/generated/flagship")
        }
    }
}

dependencies {
    // Flagship Android libraries
    implementation(projects.flagshipCore)
    implementation(projects.flagshipProviderRest)
    implementation(projects.flagshipProviderFirebase)
    implementation(projects.flagshipUiCompose)
    
    // AndroidX
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    
    // Jetpack Compose (для чистого Android проекта)
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    // Ktor for REST
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    
    // Firebase
    implementation(project.dependencies.platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.kotlinx.coroutines.play.services)
}

