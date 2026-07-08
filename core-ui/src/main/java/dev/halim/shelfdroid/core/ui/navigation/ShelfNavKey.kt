package dev.halim.shelfdroid.core.ui.navigation

import androidx.navigation3.runtime.NavKey
import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.navigation.NavEditApiKeys
import dev.halim.shelfdroid.core.navigation.NavEditUser
import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import kotlinx.serialization.Serializable

@Serializable sealed interface ShelfNavKey : NavKey

@Serializable
data class Login(val reLogin: Boolean = false, val reason: AuthPromptReason? = null) : ShelfNavKey

@Serializable data class Home(val fromLogin: Boolean) : ShelfNavKey

@Serializable data object Settings : ShelfNavKey

@Serializable data object SettingsPlayback : ShelfNavKey

@Serializable data object SettingsPlayer : ShelfNavKey

@Serializable data object SettingsNotification : ShelfNavKey

@Serializable data object SettingsPodcast : ShelfNavKey

@Serializable data object SettingsListeningSession : ShelfNavKey

@Serializable data class SearchPodcast(val libraryId: String) : ShelfNavKey

@Serializable data class AddPodcast(val payload: PodcastFeedNavPayload) : ShelfNavKey

@Serializable data class Podcast(val id: String) : ShelfNavKey

@Serializable data class Book(val id: String) : ShelfNavKey

@Serializable data class EditItem(val itemId: String) : ShelfNavKey

@Serializable data class Episode(val itemId: String, val episodeId: String) : ShelfNavKey

@Serializable data class EditEpisode(val itemId: String, val episodeId: String) : ShelfNavKey

@Serializable data class AddEpisode(val id: String) : ShelfNavKey

@Serializable data object ListeningSession : ShelfNavKey

@Serializable data object OpenSession : ShelfNavKey

@Serializable data object UsersSettings : ShelfNavKey

@Serializable data class EditUser(val payload: NavEditUser) : ShelfNavKey

@Serializable data class UserInfo(val userId: String) : ShelfNavKey

@Serializable data object ChangePassword : ShelfNavKey

@Serializable data object Libraries : ShelfNavKey

@Serializable data object NavApiKeys : ShelfNavKey

@Serializable data class EditApiKeys(val payload: NavEditApiKeys) : ShelfNavKey

@Serializable data object ServerSettings : ShelfNavKey

@Serializable data object RssFeeds : ShelfNavKey

@Serializable data object Logs : ShelfNavKey

@Serializable data object Backups : ShelfNavKey
