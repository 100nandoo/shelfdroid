package dev.halim.shelfdroid.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionHolder {
  private val _sessionId = MutableStateFlow("")
  val sessionIdFlow = _sessionId.asStateFlow()

  fun setSessionId(id: String) {
    _sessionId.value = id
  }

  fun sessionId() = _sessionId.value
}
