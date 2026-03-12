package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable data class ChangePasswordRequest(val password: String, val newPassword: String)
