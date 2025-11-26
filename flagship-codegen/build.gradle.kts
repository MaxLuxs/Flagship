plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

group = "io.maxluxs.flagship"
version = "0.1.1"

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

gradlePlugin {
    plugins {
        create("flagshipCodegen") {
            id = "io.maxluxs.flagship.codegen"
            implementationClass = "io.maxluxs.flagship.codegen.FlagshipCodegenPlugin"
            version = project.version.toString()
        }
    }
}

// Enable publishing to mavenLocal
publishing {
    repositories {
        mavenLocal()
    }
}
