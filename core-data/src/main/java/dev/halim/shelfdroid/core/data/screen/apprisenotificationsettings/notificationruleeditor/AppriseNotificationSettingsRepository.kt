package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.notificationruleeditor

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsMapper
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsMutationTarget
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleUi
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.notificationRuleTestFailureReason
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.toEnableRequest
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.toRequest
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class AppriseNotificationSettingsRepository
@Inject
constructor(
  private val api: ApiService,
  private val helper: Helper,
) {

  suspend fun load(): AppriseNotificationSettingsUiState {
    return loadSettings().getOrElse {
      AppriseNotificationSettingsUiState(state = GenericState.Failure(it.message))
    }
  }

  suspend fun saveSettings(
    uiState: AppriseNotificationSettingsUiState
  ): AppriseNotificationSettingsUiState {
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
    val settings =
      if (form.id == null) {
        api.createAppriseNotificationRule(form.toRequest())
      } else {
        api.updateAppriseNotificationRule(form.id, form.toRequest())
      }

    return settings
      .map { updatedSettings ->
        AppriseNotificationSettingsMapper.applySettings(uiState, updatedSettings) {
            helper.toReadableDate(it, includeTime = true)
          }
          .copy(
            state = GenericState.Success,
            apiState =
              AppriseNotificationSettingsApiState.Success(
                AppriseNotificationSettingsMutationTarget.NotificationRule
              ),
          )
      }
      .getOrElse {
        return uiState.copy(
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
    return api
      .deleteAppriseNotificationRule(rule.id)
      .map { updatedSettings ->
        AppriseNotificationSettingsMapper.applySettings(uiState, updatedSettings) {
            helper.toReadableDate(it, includeTime = true)
          }
          .copy(
            state = GenericState.Success,
            apiState =
              AppriseNotificationSettingsApiState.Success(
                AppriseNotificationSettingsMutationTarget.NotificationRuleDelete
              ),
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

  suspend fun enableRule(
    uiState: AppriseNotificationSettingsUiState,
    rule: NotificationRuleUi,
  ): AppriseNotificationSettingsUiState {
    return api
      .updateAppriseNotificationRule(rule.id, rule.toEnableRequest())
      .map { updatedSettings ->
        AppriseNotificationSettingsMapper.applySettings(uiState, updatedSettings) {
            helper.toReadableDate(it, includeTime = true)
          }
          .copy(
            state = GenericState.Success,
            apiState =
              AppriseNotificationSettingsApiState.Success(
                AppriseNotificationSettingsMutationTarget.NotificationRuleEnable
              ),
          )
      }
      .getOrElse {
        uiState.copy(
          apiState =
            AppriseNotificationSettingsApiState.Failure(
              target = AppriseNotificationSettingsMutationTarget.NotificationRuleEnable,
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
    val response =
      api.appriseNotificationSettings().getOrElse {
        return Result.failure(it)
      }

    return Result.success(
      AppriseNotificationSettingsMapper.map(response) {
          helper.toReadableDate(it, includeTime = true)
        }
        .copy(state = GenericState.Success)
    )
  }
}
