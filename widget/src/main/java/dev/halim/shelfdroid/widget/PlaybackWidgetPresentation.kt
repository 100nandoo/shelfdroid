package dev.halim.shelfdroid.widget

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.media.service.PlaybackService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext

internal sealed interface PlaybackWidgetPresentation {
  data object Empty : PlaybackWidgetPresentation

  data class Active(val media: CurrentPlaybackMedia) : PlaybackWidgetPresentation

  data class Error(val media: CurrentPlaybackMedia) : PlaybackWidgetPresentation
}

internal data class CurrentPlaybackMedia(
  val mediaId: String,
  val mediaTitle: String,
  val playableTitle: String,
  val artwork: Bitmap?,
)

internal data class PlaybackSessionSnapshot(
  val item: PlaybackSessionItem?,
  val playbackState: Int,
  val playWhenReady: Boolean,
  val hasError: Boolean,
)

internal data class PlaybackSessionItem(
  val mediaId: String,
  val mediaTitle: String,
  val playableTitle: String,
  val artworkData: ByteArray?,
  val artworkUri: Uri?,
)

@Singleton
internal class PlaybackWidgetPresentationLoader
@Inject
constructor(
  private val snapshotSource: Media3PlaybackSessionSnapshotSource,
  private val artworkLoader: PlaybackWidgetArtworkLoader,
) {
  suspend fun load(): PlaybackWidgetPresentation {
    val snapshot = runCatching { snapshotSource.load() }.getOrNull()
    val item = snapshot?.item ?: return PlaybackWidgetPresentation.Empty
    val artwork = runCatching { artworkLoader.load(item.artworkData, item.artworkUri) }.getOrNull()
    return mapPlaybackWidgetPresentation(snapshot, artwork)
  }
}

internal fun mapPlaybackWidgetPresentation(
  snapshot: PlaybackSessionSnapshot?,
  artwork: Bitmap?,
): PlaybackWidgetPresentation {
  val item = snapshot?.item ?: return PlaybackWidgetPresentation.Empty
  val media =
    CurrentPlaybackMedia(
      mediaId = item.mediaId,
      mediaTitle = item.mediaTitle,
      playableTitle = item.playableTitle,
      artwork = artwork,
    )
  return if (snapshot.hasError) {
    PlaybackWidgetPresentation.Error(media)
  } else {
    PlaybackWidgetPresentation.Active(media)
  }
}

@Singleton
internal class Media3PlaybackSessionSnapshotSource
@Inject
constructor(@param:ApplicationContext private val context: Context) {
  @OptIn(UnstableApi::class)
  suspend fun load(): PlaybackSessionSnapshot? =
    withContext(Dispatchers.Main.immediate) {
      val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
      val controllerFuture = MediaController.Builder(context, token).buildAsync()
      try {
        val controller = controllerFuture.await()
        PlaybackSessionSnapshot(
          item = controller.currentMediaItem?.toPlaybackSessionItem(),
          playbackState = controller.playbackState,
          playWhenReady = controller.playWhenReady,
          hasError = controller.playerError != null,
        )
      } finally {
        MediaController.releaseFuture(controllerFuture)
      }
    }
}

private fun androidx.media3.common.MediaItem.toPlaybackSessionItem(): PlaybackSessionItem {
  val metadata = mediaMetadata
  val playableTitle = metadata.title?.toString().orEmpty()
  return PlaybackSessionItem(
    mediaId = mediaId,
    mediaTitle = metadata.albumTitle?.toString().orEmpty().ifBlank { playableTitle },
    playableTitle = playableTitle,
    artworkData = metadata.artworkData,
    artworkUri = metadata.artworkUri,
  )
}

@Singleton
internal class PlaybackWidgetArtworkLoader
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val imageLoader: ImageLoader,
) {
  suspend fun load(artworkData: ByteArray?, artworkUri: Uri?): Bitmap? {
    val source = artworkData ?: artworkUri?.takeUnless { it == Uri.EMPTY } ?: return null
    return withContext(Dispatchers.IO) {
      val request =
        ImageRequest.Builder(context)
          .data(source)
          .size(PLAYBACK_WIDGET_ARTWORK_SIZE_PX)
          .allowHardware(false)
          .build()
      val result = imageLoader.execute(request) as? SuccessResult ?: return@withContext null
      result.image.toBitmap(
        width = PLAYBACK_WIDGET_ARTWORK_SIZE_PX,
        height = PLAYBACK_WIDGET_ARTWORK_SIZE_PX,
      )
    }
  }
}

private const val PLAYBACK_WIDGET_ARTWORK_SIZE_PX = 256
