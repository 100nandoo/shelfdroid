package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class AppriseNotificationSettingsRepository
@Inject
constructor(
  private val api: ApiService,
  private val helper: Helper,
) {

  suspend fun load(): AppriseNotificationSettingsUiState {
    val response =
      api.appriseNotificationSettings().getOrElse {
        return AppriseNotificationSettingsUiState(state = GenericState.Failure(it.message))
      }

    return AppriseNotificationSettingsMapper
      .map(response) { helper.toReadableDate(it, includeTime = true) }
      .copy(state = GenericState.Success)
  }
}
