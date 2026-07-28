package dev.halim.shelfdroid.core.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreatePodcastNavResult(val id: String = "", val feedUrl: String = "") : Parcelable

data object ApiKeyChangedNavResult

data object AppriseNotificationRuleChangedNavResult

object NavResultKey {
  const val CREATE_PODCAST = "create_podcast"
  const val API_KEY_CHANGED = "api_key_changed"
  const val APPRISE_NOTIFICATION_RULE_CHANGED = "apprise_notification_rule_changed"
}
