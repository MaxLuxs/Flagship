plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
    id("java-library")
    id("maven-publish")
}

group = "io.maxluxs.flagship"
version = "0.1.1"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(projects.flagshipCore)
    
    // Spring Boot
    compileOnly(libs.spring.boot.starter)
    compileOnly(libs.spring.boot.autoconfigure)
    compileOnly(libs.spring.boot.configuration.processor)
    
    // Spring Web (optional, for REST endpoints)
    compileOnly(libs.spring.boot.starter.web)
    
    // Annotation processing
    annotationProcessor(libs.spring.boot.configuration.processor)
    
    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test)
}