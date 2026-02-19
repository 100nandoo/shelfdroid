package dev.halim.shelfdroid.core.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreatePodcastNavResult(val id: String = "", val feedUrl: String = "") : Parcelable

object NavResultKey {
  const val CREATE_PODCAST = "create_podcast"
  const val UPDATE_USER = "update_user"
}
