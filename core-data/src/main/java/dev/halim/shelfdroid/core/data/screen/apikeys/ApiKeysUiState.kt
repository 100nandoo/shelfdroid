package dev.halim.shelfdroid.core.data.screen.apikeys

import dev.halim.shelfdroid.core.data.GenericState

data class ApiKeysUiState(
  val state: GenericState = GenericState.Loading,
  val apiKeys: List<ApiKeyUi> = emptyList(),
)

data class ApiKeyUi(
  val id: String,
  val name: String,
  val owner: String,
  val expiresAt: String?,
  val lastUsedAt: String?,
  val isActive: Boolean,
)
