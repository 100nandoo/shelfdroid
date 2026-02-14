package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable data class DeleteSessionsRequest(val sessions: List<String>)
