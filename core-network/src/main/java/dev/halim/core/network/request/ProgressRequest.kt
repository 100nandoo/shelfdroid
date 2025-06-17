package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable data class ProgressRequest(val isFinished: Boolean)
