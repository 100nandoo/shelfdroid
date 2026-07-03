package dev.halim.shelfdroid.core

import kotlinx.serialization.Serializable

@Serializable
enum class AuthPromptReason {
  RefreshFailed,
  ManualReLogin,
}
