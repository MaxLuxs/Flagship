import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.googleServices)
}

// Apply flagship-codegen plugin from local project
// Note: Plugin needs to be included as buildSrc or included build to work
// For now, commenting out to allow project to compile
// apply(plugin = "io.maxluxs.flagship.codegen")

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }
    
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("composeApp/src/commonMain/kotlin")
            resources.srcDirs("composeApp/src/commonMain/resources", "composeApp/src/commonMain/composeResources")
        }
        val commonTest by getting {
            kotlin.srcDirs("composeApp/src/commonTest/kotlin")
        }
        val androidMain by getting {
            kotlin.srcDirs("composeApp/src/androidMain/kotlin")
            resources.srcDirs("composeApp/src/androidMain/res")
        }
        val jvmMain by getting {
            kotlin.srcDirs("composeApp/src/jvmMain/kotlin")
        }
        val jsMain by getting {
            kotlin.srcDirs("composeApp/src/jsMain/kotlin")
            resources.srcDirs("composeApp/src/jsMain/resources")
        }
        
        // iOS source sets (iosX64Main, iosArm64Main, iosSimulatorArm64Main) automatically
        // inherit code and resources from iosMain source set
        // iosMain is created automatically when iOS targets are defined above
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(projects.flagshipProviderFirebase)
            implementation(projects.flagshipProviderLaunchdarkly)
            implementation(libs.ktor.client.android)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            
            // Firebase & LaunchDarkly
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.launchdarkly.android.client.sdk)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.config)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            
            // Flagship library
            implementation(projects.flagshipCore)
            implementation(projects.flagshipProviderRest)
            implementation(projects.flagshipUiCompose)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        
        jsMain.dependencies {
            implementation(compose.html.core)
            implementation(libs.ktor.client.js)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        
        iosMain.dependencies {
            // Platform code is now in flagship-core
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
            // Compose UI testing is experimental and requires specific setup
            // For now, we'll create basic structure tests without full UI testing framework
        }
    }
}

// Configure iOS source sets after they are created
afterEvaluate {
    kotlin.sourceSets.findByName("iosMain")?.apply {
        kotlin.srcDirs("composeApp/src/iosMain/kotlin")
        resources.srcDirs("composeApp/src/iosMain/resources")
    }
}

android {
    namespace = "io.maxluxs.flagship"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    
    sourceSets {
        getByName("main") {
            manifest.srcFile("composeApp/src/androidMain/AndroidManifest.xml")
            res.srcDirs("composeApp/src/androidMain/res")
        }
        getByName("debug") {
            res.srcDirs("composeApp/src/debug")
        }
    }

    defaultConfig {
        applicationId = "io.maxluxs.flagship"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

compose.desktop {
    application {
        mainClass = "io.maxluxs.flagship.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "Flagship Sample"
            packageVersion = "1.0.0"
            
            macOS {
                bundleID = "io.maxluxs.flagship.sample"
            }
            
            windows {
                menuGroup = "Flagship"
                upgradeUuid = "18159995-d967-4cd2-8885-77BFA97CFA9F"
            }
            
            linux {
                packageName = "flagship-sample"
            }
        }
    }
}

// Flagship codegen configuration
// Note: Commented out until plugin is properly configured
// To enable: add flagship-codegen as included build or use buildSrc
// flagshipCodegen {
//     configFile = file("flags.json")
//     outputDir = file("build/generated/flagship")
//     packageName = "io.maxluxs.flagship.generated"
// }

// Add generated sources to source sets
// Note: Commented out until codegen plugin is configured
// kotlin {
//     sourceSets {
//         val commonMain by getting {
//             kotlin.srcDirs("build/generated/flagship")
//         }
//     }
// }