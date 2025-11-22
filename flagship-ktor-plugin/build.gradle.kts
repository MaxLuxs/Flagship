plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
    id("java-library")
}

group = "io.maxluxs.flagship"
version = "0.1.1"

repositories {
    mavenCentral()
}

dependencies {
    api(projects.flagshipCore)
    
    // Ktor Server
    compileOnly(libs.ktor.server.core)
    
    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test)
}

