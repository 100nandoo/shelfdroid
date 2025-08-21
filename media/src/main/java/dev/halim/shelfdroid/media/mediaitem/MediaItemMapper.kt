package dev.halim.shelfdroid.media.mediaitem

import android.annotation.SuppressLint
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
import dev.halim.shelfdroid.core.PlayerUiState
import javax.inject.Inject

@SuppressLint("UnsafeOptInUsageError")
class MediaItemMapper @Inject constructor() {
  fun toMediaItem(uiState: PlayerUiState, state: PlayerInternalStateHolder): MediaItem {
    val isBook = state.isBook()
    val mediaType =
      if (isBook) MediaMetadata.MEDIA_TYPE_AUDIO_BOOK else MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE
    val secondaryId =
      if (isBook) {
        if (state.mediaStructure().isSingleTrack()) {
          null
        } else {
          uiState.currentTrack.index.toString()
        }
      } else {
        uiState.episodeId
      }
    val mediaIdWrapper = MediaIdWrapper(uiState.id, secondaryId)
    val mediaId = mediaIdWrapper.toMediaId()

    val currentChapter = uiState.currentChapter

    val clippingConfiguration =
      if (currentChapter != null && uiState.playerTracks.size == 1 && isBook) {
        val start = currentChapter.startTimeSeconds * 1000
        val end = currentChapter.endTimeSeconds * 1000
        MediaItem.ClippingConfiguration.Builder()
          .setStartPositionMs(start.toLong())
          .setEndPositionMs(end.toLong())
          .build()
      } else {
        MediaItem.ClippingConfiguration.UNSET
      }

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
      .setCustomCacheKey(mediaId)
      .setMediaMetadata(mediaMetadata)
      .setClippingConfiguration(clippingConfiguration)
      .build()
  }
}
