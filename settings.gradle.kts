rootProject.name = "Flagship"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        // Add mavenLocal for local plugin development
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":flagship-sample")
project(":flagship-sample").projectDir = file("samples/flagship-sample")

include(":flagship-sample-js")
project(":flagship-sample-js").projectDir = file("samples/flagship-sample-js")

include(":flagship-sample-android")
project(":flagship-sample-android").projectDir = file("samples/flagship-sample-android")

include(":flagship-sample-ios")
project(":flagship-sample-ios").projectDir = file("samples/flagship-sample-ios")

include(":flagship-sample-spring")
project(":flagship-sample-spring").projectDir = file("samples/flagship-sample-spring")

include(":flagship-sample-ktor")
project(":flagship-sample-ktor").projectDir = file("samples/flagship-sample-ktor")

include(":flagship-core")
include(":flagship-shared")
include(":flagship-provider-rest")
include(":flagship-provider-firebase")
include(":flagship-provider-launchdarkly")
include(":flagship-ui-components")
include(":flagship-ui-compose")
include(":flagship-admin-ui-compose")
include(":flagship-server")
include(":flagship-codegen")
include(":flagship-spring-boot-starter")
include(":flagship-ktor-plugin")
