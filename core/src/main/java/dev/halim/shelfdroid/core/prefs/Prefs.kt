package dev.halim.shelfdroid.core.prefs

import kotlinx.serialization.Serializable

@Serializable
data class Prefs(
  val userPrefs: UserPrefs = UserPrefs(),
  val displayPrefs: DisplayPrefs = DisplayPrefs(),
  val crudPrefs: CrudPrefs = CrudPrefs(),
)
