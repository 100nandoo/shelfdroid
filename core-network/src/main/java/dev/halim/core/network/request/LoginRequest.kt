package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable data class LoginRequest(val username: String, val password: String)
