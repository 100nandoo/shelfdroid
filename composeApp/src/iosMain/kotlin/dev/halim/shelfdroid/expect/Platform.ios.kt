package dev.halim.shelfdroid.expect

import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual abstract class PlatformContext private constructor() {
    companion object {
        val INSTANCE = object : PlatformContext() {}
    }
}