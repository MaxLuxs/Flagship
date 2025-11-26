package io.maxluxs.flagship

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

@OptIn(ExperimentalForeignApi::class)
actual fun getPlatform(): Platform = IOSPlatform()