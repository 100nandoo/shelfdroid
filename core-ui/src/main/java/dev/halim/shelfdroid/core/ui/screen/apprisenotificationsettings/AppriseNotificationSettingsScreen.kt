package dev.halim.shelfdroid.core.ui.screen.apprisenotificationsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseGlobalSettingsUi
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleUi
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleStatus
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.TextBodyMedium
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.components.TextTitleSmall
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen

@Composable
fun AppriseNotificationSettingsScreen(
  viewModel: AppriseNotificationSettingsViewModel = hiltViewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  AppriseNotificationSettingsContent(uiState = uiState)
}

@Composable
private fun AppriseNotificationSettingsContent(
  uiState: AppriseNotificationSettingsUiState = AppriseNotificationSettingsUiState()
) {
  Column(modifier = Modifier.fillMaxSize()) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    when (val state = uiState.state) {
      GenericState.Loading,
      GenericState.Idle -> Unit

      is GenericState.Failure -> {
        GenericMessageScreen(state.errorMessage ?: "")
      }

      GenericState.Success -> {
        if (!uiState.canAccess) {
          GenericMessageScreen(stringResource(R.string.apprise_notification_settings_admin_only))
          return@Column
        }
        Column(
          modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
          Spacer(modifier = Modifier.height(16.dp))
          TextTitleMedium(text = stringResource(R.string.apprise_notification_settings))
          TextLabelSmall(
            text = stringResource(R.string.apprise_notification_settings_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(16.dp))
          GlobalSettingsSection(uiState.settings)
          Spacer(modifier = Modifier.height(24.dp))
          NotificationRulesSection(uiState.notificationRules)
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

@Composable
private fun GlobalSettingsSection(settings: AppriseGlobalSettingsUi) {
  SettingValueRow(
    label = stringResource(R.string.apprise_api_url),
    value = settings.appriseApiUrl,
  )
  Spacer(modifier = Modifier.height(12.dp))
  SettingValueRow(
    label = stringResource(R.string.max_queue_size_for_notification_events),
    value = settings.maxNotificationQueue,
  )
  Spacer(modifier = Modifier.height(12.dp))
  SettingValueRow(
    label = stringResource(R.string.max_failed_attempts),
    value = settings.maxFailedAttempts,
  )
}

@Composable
private fun NotificationRulesSection(notificationRules: List<NotificationRuleUi>) {
  TextTitleMedium(text = stringResource(R.string.notification_rules))
  Spacer(modifier = Modifier.height(8.dp))

  if (notificationRules.isEmpty()) {
    TextBodyMedium(
      text = stringResource(R.string.empty_type, stringResource(R.string.notification_rules)),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return
  }

  Column {
    notificationRules.forEachIndexed { index, rule ->
      if (index > 0) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
      }
      NotificationRuleCard(rule = rule)
    }
  }
}

@Composable
private fun NotificationRuleCard(rule: NotificationRuleUi) {
  TextTitleSmall(text = rule.eventName)
  TextLabelSmall(
    text = stringResource(if (rule.enabled) R.string.enabled else R.string.disabled),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  Spacer(modifier = Modifier.height(12.dp))
  SettingValueRow(label = stringResource(R.string.destinations), value = rule.destinationSummary)
  Spacer(modifier = Modifier.height(8.dp))
  SettingValueRow(label = statusLabel(rule.status), value = statusValue(rule))
  Spacer(modifier = Modifier.height(8.dp))
  SettingValueRow(
    label = stringResource(R.string.consecutive_failed_attempts),
    value = rule.consecutiveFailedAttempts,
  )
  if (rule.titleTemplate.isNotBlank()) {
    Spacer(modifier = Modifier.height(8.dp))
    SettingValueRow(label = stringResource(R.string.title_template), value = rule.titleTemplate)
  }
  if (rule.bodyTemplate.isNotBlank()) {
    Spacer(modifier = Modifier.height(8.dp))
    SettingValueRow(label = stringResource(R.string.body_template), value = rule.bodyTemplate)
  }
}

@Composable
private fun SettingValueRow(label: String, value: String) {
  TextLabelSmall(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
  TextBodyMedium(text = value.ifBlank { "—" })
}

@Composable
private fun statusLabel(status: NotificationRuleStatus): String =
  stringResource(
    when (status) {
      NotificationRuleStatus.NeverFired -> R.string.status
      NotificationRuleStatus.LastAttemptFailed -> R.string.last_attempt_failed
      NotificationRuleStatus.LastFired -> R.string.last_fired
    }
  )

@Composable
private fun statusValue(rule: NotificationRuleUi): String =
  if (rule.status == NotificationRuleStatus.NeverFired) stringResource(R.string.never_fired)
  else rule.statusValue

@ShelfDroidPreview
@Composable
private fun AppriseNotificationSettingsContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    AppriseNotificationSettingsContent(
      uiState =
        AppriseNotificationSettingsUiState(
          state = GenericState.Success,
          settings =
            AppriseGlobalSettingsUi(
              appriseApiUrl = "https://apprise.example.com/notify",
              maxNotificationQueue = "5",
              maxFailedAttempts = "3",
            ),
          notificationRules =
            listOf(
              NotificationRuleUi(
                id = "rule-1",
                eventName = "onBackupFailed",
                enabled = true,
                destinationSummary = "discord://alerts\nmailto://ops@example.com",
                status = NotificationRuleStatus.LastFired,
                statusValue = "25 July 2026 3:45PM",
                consecutiveFailedAttempts = "0",
                titleTemplate = "Backup failed",
                bodyTemplate = "The latest backup failed on the Audiobookshelf server.",
              ),
              NotificationRuleUi(
                id = "rule-2",
                eventName = "onRSSFeedDisabled",
                enabled = false,
                destinationSummary = "mailto://reader@example.com",
                status = NotificationRuleStatus.NeverFired,
                statusValue = "",
                consecutiveFailedAttempts = "2",
                titleTemplate = "",
                bodyTemplate = "",
              ),
            ),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AppriseNotificationSettingsEmptyPreview() {
  PreviewWrapper(dynamicColor = false) {
    AppriseNotificationSettingsContent(
      uiState =
        AppriseNotificationSettingsUiState(
          state = GenericState.Success,
          settings =
            AppriseGlobalSettingsUi(
              appriseApiUrl = "",
              maxNotificationQueue = "10",
              maxFailedAttempts = "5",
            ),
        )
    )
  }
}
