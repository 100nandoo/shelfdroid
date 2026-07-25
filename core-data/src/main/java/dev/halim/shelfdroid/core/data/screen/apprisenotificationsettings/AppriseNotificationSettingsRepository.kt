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
    if (!prefsRepository.userPrefs.first().type.isAdminOrUp()) {
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
      return uiState.copy(
        apiState =
          AppriseNotificationSettingsApiState.Failure(
            target = AppriseNotificationSettingsMutationTarget.GlobalSettings,
            message = it.message,
          )
      )
    }

    return loadSettings()
      .map {
        it.copy(
          apiState =
            AppriseNotificationSettingsApiState.Success(
              AppriseNotificationSettingsMutationTarget.GlobalSettings
            )
        )
      }
      .getOrElse {
        uiState.copy(
          apiState =
            AppriseNotificationSettingsApiState.Failure(
              target = AppriseNotificationSettingsMutationTarget.GlobalSettings,
              message = it.message,
            )
        )
      }
  }

  suspend fun mutateRule(
    uiState: AppriseNotificationSettingsUiState,
    form: NotificationRuleForm,
  ): AppriseNotificationSettingsUiState {
    val mutation =
      if (form.id == null) {
        api.createAppriseNotificationRule(form.toRequest())
      } else {
        api.updateAppriseNotificationRule(form.id, form.toRequest())
      }

    mutation.getOrElse {
      return uiState.copy(
        apiState =
          AppriseNotificationSettingsApiState.Failure(
            target = AppriseNotificationSettingsMutationTarget.NotificationRule,
            message = it.message,
          )
      )
    }

    return loadSettings()
      .map {
        it.copy(
          apiState =
            AppriseNotificationSettingsApiState.Success(
              AppriseNotificationSettingsMutationTarget.NotificationRule
            )
        )
      }
      .getOrElse {
        uiState.copy(
          apiState =
            AppriseNotificationSettingsApiState.Failure(
              target = AppriseNotificationSettingsMutationTarget.NotificationRule,
              message = it.message,
            )
        )
      }
  }

  suspend fun deleteRule(
    uiState: AppriseNotificationSettingsUiState,
    rule: NotificationRuleUi,
  ): AppriseNotificationSettingsUiState {
    api.deleteAppriseNotificationRule(rule.id).getOrElse {
      return uiState.copy(
        apiState =
          AppriseNotificationSettingsApiState.Failure(
            target = AppriseNotificationSettingsMutationTarget.NotificationRuleDelete,
            message = it.message,
          )
      )
    }

    return loadSettings()
      .map {
        it.copy(
          apiState =
            AppriseNotificationSettingsApiState.Success(
              AppriseNotificationSettingsMutationTarget.NotificationRuleDelete
            )
        )
      }
      .getOrElse {
        uiState.copy(
          apiState =
            AppriseNotificationSettingsApiState.Failure(
              target = AppriseNotificationSettingsMutationTarget.NotificationRuleDelete,
              message = it.message,
            )
        )
      }
  }

  suspend fun testRule(
    uiState: AppriseNotificationSettingsUiState,
    rule: NotificationRuleUi,
  ): AppriseNotificationSettingsUiState {
    api.testAppriseNotificationRule(rule.id).getOrElse {
      return uiState.copy(
        apiState =
          AppriseNotificationSettingsApiState.Failure(
            target = AppriseNotificationSettingsMutationTarget.NotificationRuleTest,
            message = it.message,
            reason = notificationRuleTestFailureReason(it),
          )
      )
    }

    return uiState.copy(
      apiState =
        AppriseNotificationSettingsApiState.Success(
          AppriseNotificationSettingsMutationTarget.NotificationRuleTest
        )
    )
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
