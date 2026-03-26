package dev.halim.shelfdroid.core.data.screen.apikeys.edit

import dev.halim.shelfdroid.core.data.GenericState

data class EditApiKeysUiState(
  val state: GenericState = GenericState.Loading,
  val apiKeyId: String = "",
  val name: String = "",
  val users: List<User> = emptyList(),
  val selectedUserId: String = "",
  val isActive: Boolean = true,
) {
  data class User(val id: String, val username: String)
}
