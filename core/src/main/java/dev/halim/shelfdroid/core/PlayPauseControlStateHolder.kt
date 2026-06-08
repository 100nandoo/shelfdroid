package dev.halim.shelfdroid.core

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class PlayPauseControlStateHolder @Inject constructor() {
  private val state = MutableStateFlow(PlayPauseControlState())

  fun current() = state.value

  fun isPlaying() = state.value.isPlaying

  fun update(playPause: PlayPauseControlState) {
    state.update { playPause }
  }
}
