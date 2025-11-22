plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.kotlinSerialization)
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        create("flagshipCodegen") {
            id = "io.maxluxs.flagship.codegen"
            implementationClass = "io.maxluxs.flagship.codegen.FlagshipCodegenPlugin"
        }
    }
}

