package dev.halim.shelfdroid.expect

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect val manufacturer: String

expect val deviceName: String

expect val sdkVersion: Int

expect val supportedMimeType: List<String>

expect abstract class PlatformContext
