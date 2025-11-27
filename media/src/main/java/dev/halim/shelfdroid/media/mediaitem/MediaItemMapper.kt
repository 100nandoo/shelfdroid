package dev.halim.shelfdroid.media.mediaitem

import android.annotation.SuppressLint
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER
import coil3.ImageLoader
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
import dev.halim.shelfdroid.core.PlayerTrack
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.screen.player.PlayerFinder
import javax.inject.Inject
import okio.FileSystem

@SuppressLint("UnsafeOptInUsageError")
class MediaItemMapper
@Inject
constructor(private val finder: PlayerFinder, private val imageLoader: ImageLoader) {

  fun toMediaItem(uiState: PlayerUiState, state: PlayerInternalStateHolder): MediaItem {
    val isBook = state.isBook()
    val mediaType =
      if (isBook) MediaMetadata.MEDIA_TYPE_AUDIO_BOOK else MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE
    val secondaryId = if (isBook) null else uiState.episodeId
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

    val artworkData: ByteArray? = readDiskCachedImage(uiState.cover)

    val mediaMetadata =
      MediaMetadata.Builder()
        .setTitle(uiState.title)
        .setArtist(uiState.author)
        .setArtworkUri(uiState.cover.toUri())
        .setArtworkData(artworkData, PICTURE_TYPE_FRONT_COVER)
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

  fun toMediaItemList(uiState: PlayerUiState): List<MediaItem> {
    val currentChapter = uiState.currentChapter
    val tracks = finder.tracksFromChapter(uiState.playerTracks, currentChapter!!)

    val artworkData: ByteArray? = readDiskCachedImage(uiState.cover)

    val mediaMetadata =
      MediaMetadata.Builder()
        .setTitle(uiState.title)
        .setArtist(uiState.author)
        .setArtworkUri(uiState.cover.toUri())
        .setArtworkData(artworkData, PICTURE_TYPE_FRONT_COVER)
        .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
        .build()

    val mediaItems =
      tracks.mapIndexed { index: Int, track: PlayerTrack ->
        val mediaIdWrapper = MediaIdWrapper(uiState.id, track.index.toString())
        val mediaId = mediaIdWrapper.toMediaId()

        val clip = MediaItem.ClippingConfiguration.Builder()
        if (index == 0) {
          val start = currentChapter.startTimeSeconds - track.startOffset
          clip.setStartPositionMs((start * 1000).toLong())
        }
        if (index == tracks.size - 1) {
          val trackEndTime = track.startOffset + track.duration
          val diff = trackEndTime - currentChapter.endTimeSeconds
          val end = trackEndTime - diff
          clip.setEndPositionMs((end * 1000).toLong())
        }
        MediaItem.Builder()
          .setUri(track.url)
          .setMediaId(mediaId)
          .setCustomCacheKey(mediaId)
          .setMediaMetadata(mediaMetadata)
          .setClippingConfiguration(clip.build())
          .build()
      }

    return mediaItems
  }

  private fun readDiskCachedImage(cover: String): ByteArray? {
    val artworkPath = imageLoader.diskCache?.openSnapshot(cover)?.data
    val fileSystem = FileSystem.SYSTEM
    var artworkData: ByteArray? = null
    artworkPath?.let { artworkData = fileSystem.read(artworkPath) { readByteArray() } }
    return artworkData
  }
}
