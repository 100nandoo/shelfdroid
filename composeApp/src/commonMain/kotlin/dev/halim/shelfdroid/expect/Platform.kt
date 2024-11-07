package dev.halim.shelfdroid.expect

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform