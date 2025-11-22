package io.maxluxs.flagship.helpers

import io.maxluxs.flagship.core.model.EvalContext

/**
 * Additional helper functions for tests
 */
object TestHelpers {
    
    fun createTestContext(
        userId: String = "test_user",
        deviceId: String = "test_device",
        appVersion: String = "1.0.0",
        region: String = "US"
    ): EvalContext {
        return EvalContext(
            userId = userId,
            deviceId = deviceId,
            appVersion = appVersion,
            osName = "Test",
            osVersion = "1.0",
            locale = "en_US",
            region = region
        )
    }
    
    fun createContextForVersion(version: String): EvalContext {
        return createTestContext(appVersion = version)
    }
    
    fun createContextForRegion(region: String): EvalContext {
        return createTestContext(region = region)
    }
}

