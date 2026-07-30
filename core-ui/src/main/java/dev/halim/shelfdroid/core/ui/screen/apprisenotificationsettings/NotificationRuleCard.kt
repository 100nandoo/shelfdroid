package dev.halim.shelfdroid.core.ui.screen.apprisenotificationsettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleStatus
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleUi
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.extensions.enableAlpha
import dev.halim.shelfdroid.core.ui.mySharedElement
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
internal fun NotificationRuleCard(
  rule: NotificationRuleUi,
  enabled: Boolean,
  onEnable: () -> Unit,
  onTest: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(enabled = enabled, onClick = onEdit)
        .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    Text(
      text = rule.eventName,
      style = MaterialTheme.typography.bodyLarge,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier =
        Modifier.alpha(rule.enabled.enableAlpha())
          .mySharedElement(Animations.Companion.NotificationRule.eventNameKey(rule.id)),
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
    ) {
      Text(
        text = rule.destinationSummary,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
          Modifier.weight(1f)
            .alpha(rule.enabled.enableAlpha())
            .mySharedElement(Animations.Companion.NotificationRule.destinationKey(rule.id)),
      )
      if (rule.enabled) {
        FilledTonalIconButton(enabled = enabled, onClick = onTest) {
          Icon(
            painter = painterResource(R.drawable.test),
            contentDescription = stringResource(R.string.test),
          )
        }
      } else {
        FilledTonalIconButton(enabled = enabled, onClick = onEnable) {
          Icon(
            painter = painterResource(R.drawable.mode_off_on),
            contentDescription = stringResource(R.string.enable),
          )
        }
      }
      FilledTonalIconButton(enabled = enabled, onClick = onEdit) {
        Icon(
          painter = painterResource(R.drawable.edit),
          contentDescription = stringResource(R.string.edit),
        )
      }
      FilledTonalIconButton(enabled = enabled, onClick = onDelete) {
        Icon(
          painter = painterResource(R.drawable.delete),
          contentDescription = stringResource(R.string.delete),
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun NotificationRuleCardPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    NotificationRuleCard(
      rule =
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
      enabled = true,
      onEnable = {},
      onTest = {},
      onEdit = {},
      onDelete = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun DisabledNotificationRuleCardPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    NotificationRuleCard(
      rule =
        NotificationRuleUi(
          id = "rule-2",
          eventName = "onPodcastEpisodeDownloaded",
          enabled = false,
          destinationSummary = "ntfy://devices/mobile-app",
          status = NotificationRuleStatus.LastFired,
          statusValue = "24 July 2026 10:15AM",
          consecutiveFailedAttempts = "2",
          titleTemplate = "Episode downloaded",
          bodyTemplate = "A new podcast episode finished downloading.",
        ),
      enabled = true,
      onEnable = {},
      onTest = {},
      onEdit = {},
      onDelete = {},
    )
  }
}
