package dev.halim.shelfdroid.core.data.screen.apikeys

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

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
      apiState = ApiKeysApiState.Idle,
      apiKeys =
        response.apiKeys.map { apiKey ->
          ApiKeyUi(
            id = apiKey.id,
            name = apiKey.name,
            owner = apiKey.user.username,
            expiresAt = helper.toReadableDate(apiKey.expiresAt, true),
            lastUsedAt = helper.toReadableDate(apiKey.lastUsedAt, true),
            isExpired = apiKey.expiresAt.isExpired(),
            isActive = apiKey.isActive,
          )
        },
    )
  }

  suspend fun deleteApiKey(apiKeyId: String, uiState: ApiKeysUiState): ApiKeysUiState {
    api.deleteApiKey(apiKeyId).getOrElse {
      return uiState.copy(apiState = ApiKeysApiState.DeleteFailure(it.message))
    }

    return uiState.copy(
      apiState = ApiKeysApiState.DeleteSuccess,
      apiKeys = uiState.apiKeys.filter { it.id != apiKeyId },
    )
  }

  private fun String?.isExpired(): Boolean {
    if (this.isNullOrBlank()) return false
    return runCatching { Instant.parse(this) < Clock.System.now() }.getOrDefault(false)
  }
}
