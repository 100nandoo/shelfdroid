package dev.halim.shelfdroid.core.data.screen.apikeys.createedit

import dev.halim.shelfdroid.core.data.GenericState

data class CreateEditApiKeysUiState(
  val state: GenericState = GenericState.Loading,
  val fieldError: CreateApiKeyFieldError = CreateApiKeyFieldError.None,
  val apiKeyId: String = "",
  val name: String = "",
  val users: List<User> = emptyList(),
  val selectedUserId: String = "",
  val isActive: Boolean = true,
  val neverExpires: Boolean = true,
  val expiresAtMillis: Long = 0,
) {
  data class User(val id: String, val username: String)
}

data class CreateApiKeyFieldError(
  val nameEmpty: Boolean = false,
  val userNotSelected: Boolean = false,
  val expiresAtEmpty: Boolean = false,
) {
  val hasError: Boolean
    get() = nameEmpty || userNotSelected || expiresAtEmpty

  companion object {
    val None = CreateApiKeyFieldError()
  }
}
