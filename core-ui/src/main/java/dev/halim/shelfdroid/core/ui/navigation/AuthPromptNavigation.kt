package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.AuthPromptReason

fun AuthPromptReason?.toLoginKey(): Login {
  return if (this == null) Login() else Login(reLogin = true, reason = this)
}
