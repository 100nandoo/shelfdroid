package dev.halim.shelfdroid.widget

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.media.service.CUSTOM_NEXT_CHAPTER
import dev.halim.shelfdroid.media.service.CUSTOM_PREVIOUS_CHAPTER
import dev.halim.shelfdroid.media.service.PlaybackService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext

internal sealed interface PlaybackWidgetPresentation {
  data object Empty : PlaybackWidgetPresentation

  data class Active(
    val media: CurrentPlaybackMedia,
    val controls: PrimaryPlaybackControls,
    val chapterControls: ChapterPlaybackControls,
  ) : PlaybackWidgetPresentation

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
  val playPauseEnabled: Boolean = true,
  val seekBackEnabled: Boolean = true,
  val seekForwardEnabled: Boolean = true,
  val previousChapterEnabled: Boolean,
  val nextChapterEnabled: Boolean,
)

internal data class PrimaryPlaybackControls(
  val showPause: Boolean,
  val playPauseEnabled: Boolean,
  val seekBackEnabled: Boolean,
  val seekForwardEnabled: Boolean,
)

internal data class ChapterPlaybackControls(
  val previousEnabled: Boolean,
  val nextEnabled: Boolean,
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
    PlaybackWidgetPresentation.Active(
      media = media,
      controls =
        PrimaryPlaybackControls(
          showPause =
            snapshot.playWhenReady && snapshot.playbackState != Player.STATE_ENDED,
          playPauseEnabled = snapshot.playPauseEnabled,
          seekBackEnabled = snapshot.seekBackEnabled,
          seekForwardEnabled = snapshot.seekForwardEnabled,
        ),
      chapterControls =
        ChapterPlaybackControls(
          previousEnabled = snapshot.previousChapterEnabled,
          nextEnabled = snapshot.nextChapterEnabled,
        ),
    )
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
          playPauseEnabled = controller.isCommandAvailable(Player.COMMAND_PLAY_PAUSE),
          seekBackEnabled = controller.isCommandAvailable(Player.COMMAND_SEEK_BACK),
          seekForwardEnabled =
            controller.isCommandAvailable(Player.COMMAND_SEEK_FORWARD),
          previousChapterEnabled =
            controller.isSessionCommandAvailable(
              SessionCommand(CUSTOM_PREVIOUS_CHAPTER, Bundle.EMPTY)
            ),
          nextChapterEnabled =
            controller.isSessionCommandAvailable(
              SessionCommand(CUSTOM_NEXT_CHAPTER, Bundle.EMPTY)
            ),
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
