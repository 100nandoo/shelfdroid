package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class AppriseNotificationSettingsRepository
@Inject
constructor(
  private val api: ApiService,
  private val helper: Helper,
  private val prefsRepository: PrefsRepository,
) {

  suspend fun load(): AppriseNotificationSettingsUiState {
    if (!prefsRepository.userPrefs.first().isAdmin) {
      return AppriseNotificationSettingsUiState(
        state = GenericState.Success,
        canAccess = false,
      )
    }

    return loadSettings().getOrElse {
      AppriseNotificationSettingsUiState(state = GenericState.Failure(it.message))
    }
  }

  suspend fun saveSettings(uiState: AppriseNotificationSettingsUiState): AppriseNotificationSettingsUiState {
    if (!uiState.hasChanges) {
      return uiState.copy(apiState = AppriseNotificationSettingsApiState.Idle)
    }

    api.updateAppriseNotificationSettings(uiState.draftSettings.toRequest()).getOrElse {
      return uiState.copy(apiState = AppriseNotificationSettingsApiState.Failure(it.message))
    }

    return loadSettings()
      .map { it.copy(apiState = AppriseNotificationSettingsApiState.Success) }
      .getOrElse {
        uiState.copy(
          apiState = AppriseNotificationSettingsApiState.Success,
          savedSettings = uiState.draftSettings,
          draftSettings = uiState.draftSettings,
        )
      }
  }

  private suspend fun loadSettings(): Result<AppriseNotificationSettingsUiState> {
    val response = api.appriseNotificationSettings().getOrElse { return Result.failure(it) }

    return Result.success(
      AppriseNotificationSettingsMapper
        .map(response) { helper.toReadableDate(it, includeTime = true) }
        .copy(state = GenericState.Success)
    )
  }
}
