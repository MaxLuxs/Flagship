package io.maxluxs.flagship.spring

import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.FlagsConfigBuilder
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Auto-configuration for Flagship feature flags.
 * 
 * Automatically configures Flagship based on application properties.
 */
@AutoConfiguration
@EnableConfigurationProperties(FlagshipProperties::class)
class FlagshipAutoConfiguration(
    private val properties: FlagshipProperties
) {
    
    @Bean
    @ConditionalOnMissingBean
    fun flagsManager(providers: List<FlagsProvider>): FlagsManager {
        val config = FlagsConfigBuilder.build(
            appKey = properties.appKey ?: "spring-app",
            environment = properties.environment ?: "production",
            providers = providers.ifEmpty { emptyList() }
        )
        
        return FlagsConfigBuilder.initializeIfNeeded(config)
    }
}

