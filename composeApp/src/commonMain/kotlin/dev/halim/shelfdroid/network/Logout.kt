package dev.halim.shelfdroid.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LogoutResponse(
    @SerialName("redirect_url")
    val redirectUrl: String? = null
)