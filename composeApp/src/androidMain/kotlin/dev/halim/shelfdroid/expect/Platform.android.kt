package dev.halim.shelfdroid.expect

import android.content.Context
import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual typealias PlatformContext = Context

actual fun getDeviceName(): String {
    val deviceName = Build.MODEL
    val manufacturer = Build.MANUFACTURER
    val deviceFullName = "$manufacturer $deviceName"
    return deviceFullName
}

actual fun sdkVersion(): Int {
    return Build.VERSION.SDK_INT
}

actual fun manufacturer(): String = Build.MANUFACTURER
actual val supportedMimeType: List<String> = listOf(
    "audio/flac",
    "audio/mp4",
    "audio/aac",
    "audio/mpeg",
    "audio/mp3",
    "audio/webm",
    "audio/ac3",
    "audio/opus",
    "audio/vorbis"
)