package io.maxluxs.flagship.samplespring

import io.maxluxs.flagship.core.manager.FlagsManager
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

/**
 * Spring Boot sample application demonstrating Flagship feature flags usage.
 */
@SpringBootApplication
class SampleSpringApplication

fun main(args: Array<String>) {
    runApplication<SampleSpringApplication>(*args)
}

/**
 * Bootstrap Flagship on application startup.
 */
@Component
class FlagshipBootstrap(
    private val flagsManager: FlagsManager
) : CommandLineRunner {
    
    override fun run(vararg args: String?) {
        runBlocking {
            println("Bootstrapping Flagship...")
            val success = flagsManager.ensureBootstrap(timeoutMs = 5000)
            if (success) {
                println("✓ Flagship bootstrap successful")
            } else {
                println("⚠ Flagship bootstrap timed out, using cached/default values")
            }
        }
    }
}

