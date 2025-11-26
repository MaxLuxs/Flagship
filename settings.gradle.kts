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
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":sample")
include(":flagship-core")
include(":flagship-provider-rest")
include(":flagship-provider-firebase")
include(":flagship-provider-launchdarkly")
include(":flagship-ui-components")
include(":flagship-ui-compose")
include(":flagship-platform-android")
include(":flagship-platform-ios")
include(":flagship-ktor-plugin")
include(":flagship-spring-boot-starter")

// Internal modules from submodule (only available if submodule is initialized)
if (file("internal/flagship-server").exists()) {
    include(":flagship-server")
    project(":flagship-server").projectDir = file("internal/flagship-server")
}

if (file("internal/flagship-admin-ui-compose").exists()) {
    include(":flagship-admin-ui-compose")
    project(":flagship-admin-ui-compose").projectDir = file("internal/flagship-admin-ui-compose")
}

if (file("internal/flagship-shared").exists()) {
    include(":flagship-shared")
    project(":flagship-shared").projectDir = file("internal/flagship-shared")
}
