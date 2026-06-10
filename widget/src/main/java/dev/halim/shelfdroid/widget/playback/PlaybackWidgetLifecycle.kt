package dev.halim.shelfdroid.widget.playback

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.media.service.PlayerStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class PlaybackWidgetLifecycle
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val playerStore: PlayerStore,
  private val settingsRepository: SettingsRepository,
  @Named("io") private val scope: CoroutineScope,
) {
  private val started = AtomicBoolean(false)

  fun start() {
    if (!started.compareAndSet(false, true)) return

    scope.launch {
      combine(
          playerStore.uiState.map(::playbackSignal).distinctUntilChanged(),
          settingsRepository.darkMode.distinctUntilChanged(),
          settingsRepository.dynamicTheme.distinctUntilChanged(),
        ) { playback, darkMode, dynamicTheme ->
          RefreshSignal(playback = playback, darkMode = darkMode, dynamicTheme = dynamicTheme)
        }
        .distinctUntilChanged()
        .collect {
          runCatching { PlaybackWidget().updateAll(context) }
            .onFailure { error -> Log.e(TAG, "Failed to refresh playback widget", error) }
        }
    }
  }

  private fun playbackSignal(uiState: PlayerUiState): PlaybackSignal {
    val state =
      when {
        uiState.id.isBlank() || uiState.state is PlayerState.Hidden -> WidgetPlaybackState.Empty
        uiState.playPause.showLoadingIndicator -> WidgetPlaybackState.Loading
        uiState.playPause.showPlayIcon -> WidgetPlaybackState.Paused
        else -> WidgetPlaybackState.Playing
      }

    return PlaybackSignal(
      id = uiState.id,
      title = uiState.title,
      author = uiState.author,
      cover = uiState.cover,
      state = state,
    )
  }

  private data class RefreshSignal(
    val playback: PlaybackSignal,
    val darkMode: Boolean,
    val dynamicTheme: Boolean,
  )

  private data class PlaybackSignal(
    val id: String,
    val title: String,
    val author: String,
    val cover: String,
    val state: WidgetPlaybackState,
  )

  private enum class WidgetPlaybackState {
    Empty,
    Loading,
    Paused,
    Playing,
  }

  private companion object {
    const val TAG = "PlaybackWidget"
  }
}
