package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable data class SyncSessionRequest(val currentTime: Long, val timeListened: Long)
