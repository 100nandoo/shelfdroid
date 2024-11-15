package dev.halim.shelfdroid.network

data class SyncProgressRequest(
    val currentTime: Double,
    val timeListened: Int,
    val duration: Double,
)