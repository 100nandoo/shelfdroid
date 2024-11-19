package dev.halim.shelfdroid.network

import kotlinx.serialization.Serializable

@Serializable
data class SyncSessionRequest(
    val currentTime: Long,
    val timeListened: Long,
)