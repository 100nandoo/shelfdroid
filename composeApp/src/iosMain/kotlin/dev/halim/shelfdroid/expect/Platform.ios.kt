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

actual fun getDeviceName(): String {
    return UIDevice.currentDevice.model
}

actual fun sdkVersion(): Int {
    return runCatching { UIDevice.currentDevice.systemVersion.toDouble().toInt() }.getOrElse { 0 }
}

actual fun manufacturer(): String = "Apple"
actual val supportedMimeType: List<String> = emptyList()