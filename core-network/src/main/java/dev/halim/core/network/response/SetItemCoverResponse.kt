package dev.halim.core.network.response

import kotlinx.serialization.Serializable

@Serializable data class SetItemCoverResponse(val success: Boolean, val cover: String? = null)
