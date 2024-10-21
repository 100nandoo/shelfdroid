package dev.halim.shelfdroid

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform