package dev.halim.shelfdroid.core.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.core.data.screen.player.PlayerState
import dev.halim.shelfdroid.core.data.screen.player.PlayerUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel
@Inject
constructor(
  private val playerRepository: PlayerRepository,
  @ApplicationContext private val context: Context,
) : ViewModel() {

  private val _uiState = MutableStateFlow(PlayerUiState())

  val uiState: StateFlow<PlayerUiState> =
    _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerUiState())

  val player: Player by lazy { ExoPlayer.Builder(context).build() }

  fun onEvent(event: PlayerEvent) {
    when (event) {
      is PlayerEvent.PlayBook -> {
        viewModelScope.launch {
          _uiState.update { playerRepository.playBook(event.id) }
          playContent()
        }
      }
      is PlayerEvent.PlayPodcast -> {
        viewModelScope.launch {
          _uiState.update { playerRepository.playPodcast(event.itemId, event.episodeId) }
          playContent()
        }
      }
      PlayerEvent.Big -> _uiState.update { it.copy(state = PlayerState.Big) }
      PlayerEvent.Small -> _uiState.update { it.copy(state = PlayerState.Small) }
      PlayerEvent.TempHidden -> _uiState.update { it.copy(state = PlayerState.TempHidden) }
      PlayerEvent.Hidden -> _uiState.update { it.copy(state = PlayerState.Hidden()) }
    }
  }

  private fun playContent() {
    player.apply {
      setMediaItem(MediaItem.fromUri(_uiState.value.currentTrack.url))
      val positionMs =
        (_uiState.value.currentTime - _uiState.value.currentTrack.startOffset).toLong() * 1000
      seekTo(positionMs)
      prepare()
      play()
    }
  }
}

sealed class PlayerEvent {
  class PlayBook(val id: String) : PlayerEvent()

  class PlayPodcast(val itemId: String, val episodeId: String) : PlayerEvent()

  data object Big : PlayerEvent()

  data object Small : PlayerEvent()

  data object TempHidden : PlayerEvent()

  data object Hidden : PlayerEvent()
}
