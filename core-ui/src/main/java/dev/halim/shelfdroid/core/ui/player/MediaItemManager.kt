package dev.halim.shelfdroid.core.ui.player

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.halim.shelfdroid.core.data.screen.player.PlayerUiState
import dev.halim.shelfdroid.core.ui.navigation.MediaIdWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaItemManager @Inject constructor() {
  fun toMediaItem(uiState: PlayerUiState): MediaItem {
    val mediaIdWrapper = MediaIdWrapper(uiState.id, uiState.episodeId.takeIf { it.isNotBlank() })
    val isBook = uiState.episodeId.isBlank()
    val mediaType =
      if (isBook) MediaMetadata.MEDIA_TYPE_AUDIO_BOOK else MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE
    val mediaId = mediaIdWrapper.toMediaId()
    val mediaMetadata =
      MediaMetadata.Builder()
        .setTitle(uiState.title)
        .setArtist(uiState.author)
        .setArtworkUri(uiState.cover.toUri())
        .setMediaType(mediaType)
        .build()
    return MediaItem.Builder()
      .setUri(uiState.currentTrack.url)
      .setMediaId(mediaId)
      .setMediaMetadata(mediaMetadata)
      .build()
  }
}
