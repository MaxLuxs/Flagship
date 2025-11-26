plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "io.maxluxs.flagship"
version = "0.1.1"

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
    implementation("io.ktor:ktor-server-default-headers:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-status-pages:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-auth:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-auth-jwt:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-websockets:${libs.versions.ktor.get()}")
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.logback.classic)
    
    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    
    // Authentication
    implementation(libs.jwt)
    implementation(libs.bcrypt)
    
    // Metrics
    implementation(libs.prometheus.simpleclient)
    implementation(libs.prometheus.simpleclient.common)
    
    // Core Flagship (for models)
    implementation(projects.flagshipCore)
    implementation(projects.flagshipShared)
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
    dependsOn(":flagship-admin-ui-compose:copyWebBuildToServer")
}

// Ensure admin UI is built before building the server
tasks.named("build") {
    dependsOn(":flagship-admin-ui-compose:copyWebBuildToServer")
}

