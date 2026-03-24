package dev.halim.shelfdroid.core.data.screen.apikeys

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class ApiKeysRepository
@Inject
constructor(private val api: ApiService, private val helper: Helper) {

  suspend fun apiKeys(): ApiKeysUiState {
    val response =
      api.apiKeys().getOrElse {
        return ApiKeysUiState(state = GenericState.Failure(it.message))
      }

    return ApiKeysUiState(
      state = GenericState.Success,
      apiKeys =
        response.apiKeys.map { apiKey ->
          ApiKeyUi(
            id = apiKey.id,
            name = apiKey.name,
            owner = apiKey.user.username,
            expiresAt = helper.toReadableDate(apiKey.expiresAt, true),
            lastUsedAt = helper.toReadableDate(apiKey.lastUsedAt, true),
            isActive = apiKey.isActive,
          )
        },
    )
  }
}
