plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "io.maxluxs.flagship"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-netty:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-content-negotiation:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-cors:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-call-logging:${libs.versions.ktor.get()}")
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Logging
    implementation(libs.logback.classic)
    
    // Core Flagship (for models)
    implementation(projects.flagshipCore)
    implementation(projects.flagshipProviderRest)
    
    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation("io.ktor:ktor-server-test-host:${libs.versions.ktor.get()}")
}

application {
    mainClass.set("io.maxluxs.flagship.server.ApplicationKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

