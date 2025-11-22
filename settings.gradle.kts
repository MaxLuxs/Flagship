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
include(":flagship-ui-compose")
include(":flagship-server")
include(":flagship-codegen")
include(":flagship-spring-boot-starter")
include(":flagship-ktor-plugin")
