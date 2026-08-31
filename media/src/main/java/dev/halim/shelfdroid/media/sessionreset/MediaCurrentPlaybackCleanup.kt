package dev.halim.shelfdroid.media.sessionreset

import dagger.Lazy
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dev.halim.shelfdroid.core.data.sessionreset.CurrentPlaybackCleanup
import dev.halim.shelfdroid.media.di.MediaControllerManager
import dev.halim.shelfdroid.media.presentation.PlaybackPresentationNotifier
import dev.halim.shelfdroid.media.service.PlayerStore
import javax.inject.Inject
import kotlinx.coroutines.flow.update

@ActivityRetainedScoped
class MediaCurrentPlaybackCleanup
internal constructor(
  private val clearPlayerStore: () -> Unit,
  private val clearMediaController: () -> Unit,
  private val notifyPresentationChanged: suspend () -> Unit,
) : CurrentPlaybackCleanup {

  @Inject
  constructor(
    mediaControllerManager: Lazy<MediaControllerManager>,
    playerStore: PlayerStore,
    playbackPresentationNotifier: PlaybackPresentationNotifier,
  ) : this(
    clearPlayerStore = { playerStore.uiState.update { playerStore.emptyState() } },
    clearMediaController = { mediaControllerManager.get().clearAndStop() },
    notifyPresentationChanged = playbackPresentationNotifier::notifyPresentationChanged,
  )

  override suspend fun clearCurrentPlayback() {
    clearPlayerStore()
    clearMediaController()
    notifyPresentationChanged()
  }
}
