package dev.halim.shelfdroid.core.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreatePodcastNavResult(val id: String = "", val feedUrl: String = "") : Parcelable

data object ApiKeyChangedNavResult

@Parcelize data class LibraryChangedNavResult(val id: String = "") : Parcelable

@Parcelize
data class AppriseNotificationRuleChangedNavResult(
  val notificationRules: List<AppriseNotificationRuleNavResult> = emptyList()
) : Parcelable

@Parcelize
data class AppriseNotificationRuleNavResult(
  val id: String = "",
  val libraryId: String? = null,
  val eventName: String = "",
  val urls: List<String> = emptyList(),
  val titleTemplate: String = "",
  val bodyTemplate: String = "",
  val enabled: Boolean = true,
  val type: String? = null,
  val destinationSummary: String = "",
  val statusName: String = "NeverFired",
  val statusValue: String = "",
  val consecutiveFailedAttempts: String = "0",
) : Parcelable

object NavResultKey {
  const val CREATE_PODCAST = "create_podcast"
  const val API_KEY_CHANGED = "api_key_changed"
  const val LIBRARY_CHANGED = "library_changed"
  const val APPRISE_NOTIFICATION_RULE_CHANGED = "apprise_notification_rule_changed"
}
