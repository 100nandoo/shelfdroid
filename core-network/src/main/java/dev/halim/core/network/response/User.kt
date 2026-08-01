package dev.halim.core.network.response

import dev.halim.core.network.response.login.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class CreateUserResponse(@SerialName("user") val user: User = User())

@Serializable data class UsersResponse(@SerialName("users") val users: List<User> = emptyList())

@Serializable
data class UpdateUserResponse(
  @SerialName("success") val success: Boolean = false,
  @SerialName("user") val user: User = User(),
)

@Serializable data class DeleteUserResponse(@SerialName("success") val success: Boolean = false)
