package dev.halim.shelfdroid.core.data.screen.apikeys

import dev.halim.shelfdroid.core.data.GenericState

data class ApiKeysUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: ApiKeysApiState = ApiKeysApiState.Idle,
  val apiKeys: List<ApiKeyUi> = emptyList(),
)

data class ApiKeyUi(
  val id: String,
  val name: String,
  val owner: String,
  val expiresAt: String?,
  val lastUsedAt: String?,
  val isExpired: Boolean,
  val isActive: Boolean,
)

sealed interface ApiKeysApiState {
  data object Idle : ApiKeysApiState

  data object Loading : ApiKeysApiState

  data object DeleteSuccess : ApiKeysApiState

  data class DeleteFailure(val message: String?) : ApiKeysApiState
}
