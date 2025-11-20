plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dokka)
    // Publishing: uncomment when ready to publish
    // `maven-publish`
    // signing
}

// apply(from = rootProject.file("gradle/publish.gradle.kts"))

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "FlagshipPlatformIOS"
            isStatic = true
        }
    }

    sourceSets {
        iosMain.dependencies {
            implementation(projects.flagshipCore)
        }
    }
}

