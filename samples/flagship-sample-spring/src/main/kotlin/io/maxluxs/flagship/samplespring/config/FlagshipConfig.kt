package io.maxluxs.flagship.samplespring.config

import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.samplespring.service.MockFlagsProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Flagship providers.
 * 
 * Registers MockFlagsProvider as a Spring bean, which will be automatically
 * picked up by FlagshipAutoConfiguration.
 */
@Configuration
class FlagshipConfig {
    
    @Bean
    fun mockFlagsProvider(): FlagsProvider {
        return MockFlagsProvider()
    }
}

