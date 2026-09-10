package dev.halim.shelfdroid.core.prefs

import kotlinx.serialization.Serializable

@Serializable
data class ListeningSessionPrefs(val itemsPerPage: Int = 10, val defaultUserId: String? = null)

enum class ItemsPerPage(val label: Int) {
  I10(10),
  I25(25),
  I50(50),
  I100(100);

  companion object {
    fun fromLabel(label: Int): ItemsPerPage {
      return when (label) {
        10 -> I10
        25 -> I25
        50 -> I50
        100 -> I100
        else -> I10
      }
    }
  }
}
