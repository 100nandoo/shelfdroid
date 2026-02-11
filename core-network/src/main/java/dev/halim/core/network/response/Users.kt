package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class UsersResponse(@SerialName("users") val users: List<User> = emptyList())
