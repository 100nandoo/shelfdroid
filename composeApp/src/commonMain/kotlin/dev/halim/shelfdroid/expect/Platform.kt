package dev.halim.shelfdroid.expect

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun manufacturer(): String

expect fun getDeviceName(): String

expect fun sdkVersion(): Int

expect val supportedMimeType: List<String>

expect abstract class PlatformContext
