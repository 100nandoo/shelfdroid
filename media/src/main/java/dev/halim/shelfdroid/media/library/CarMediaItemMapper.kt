package dev.halim.shelfdroid.media.library

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.LibraryItemCatalog
import dev.halim.shelfdroid.core.database.ProgressEntity
import dev.halim.shelfdroid.media.R
import javax.inject.Inject

@SuppressLint("UnsafeOptInUsageError")
internal class CarMediaItemMapper
@Inject
constructor(@ApplicationContext private val context: Context) {

  private val fallbackArtwork: Uri by lazy {
    val resources = context.resources
    val icon = R.drawable.ic_notification
    Uri.Builder()
      .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
      .authority(resources.getResourcePackageName(icon))
      .appendPath(resources.getResourceTypeName(icon))
      .appendPath(resources.getResourceEntryName(icon))
      .build()
  }

  fun folder(id: CarMediaId.Folder): MediaItem {
    val (title, mediaType) =
      when (id) {
        CarMediaId.Root -> R.string.car_catalog_root to MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
        CarMediaId.ContinueListening ->
          R.string.car_continue_listening to MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
        CarMediaId.Books -> R.string.car_books to MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS
        CarMediaId.Podcasts -> R.string.car_podcasts to MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS
        CarMediaId.Downloads -> R.string.car_downloads to MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
      }
    return item(id, context.getString(title), mediaType, playable = false)
  }

  fun library(library: LibraryEntity): MediaItem =
    item(
      id = CarMediaId.Library(library.id),
      title = library.name,
      mediaType =
        if (library.isBookLibrary == 1L) MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS
        else MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS,
      playable = false,
    )

  fun libraryItem(record: LibraryItemCatalog, progress: ProgressEntity? = null): MediaItem {
    val isBook = record.isBook == 1L
    return item(
      id = if (isBook) CarMediaId.Book(record.id) else CarMediaId.Podcast(record.id),
      title = record.title,
      mediaType =
        if (isBook) MediaMetadata.MEDIA_TYPE_AUDIO_BOOK else MediaMetadata.MEDIA_TYPE_PODCAST,
      playable = isBook,
      artist = record.author,
      artwork = artworkFor(record),
      progress = progress,
    )
  }

  fun episode(
    podcast: LibraryItemCatalog,
    episode: PodcastEpisode,
    progress: ProgressEntity? = null,
  ): MediaItem =
    item(
      id = CarMediaId.Episode(podcast.id, episode.id),
      title = episode.title,
      mediaType = MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE,
      playable = true,
      artist = podcast.author,
      subtitle = podcast.title,
      artwork = artworkFor(podcast),
      progress = progress,
    )

  private fun artworkFor(record: LibraryItemCatalog): Uri =
    if (record.cover.isBlank()) fallbackArtwork else CarArtworkProvider.uriFor(context, record.id)

  private fun item(
    id: CarMediaId,
    title: String,
    mediaType: Int,
    playable: Boolean,
    artist: String? = null,
    subtitle: String? = null,
    artwork: Uri = fallbackArtwork,
    progress: ProgressEntity? = null,
  ): MediaItem {
    val metadata =
      MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist?.takeUnless(String::isBlank))
        .setSubtitle(subtitle?.takeUnless(String::isBlank))
        .setMediaType(mediaType)
        .setIsBrowsable(!playable)
        .setIsPlayable(playable)
        .setArtworkUri(artwork)
        .setExtras(if (playable) completionExtras(progress) else null)
        .build()
    return MediaItem.Builder().setMediaId(id.value).setMediaMetadata(metadata).build()
  }

  private fun completionExtras(progress: ProgressEntity?): Bundle {
    val fraction = progress?.progress?.takeIf(Double::isFinite)?.coerceIn(0.0, 1.0) ?: 0.0
    val status =
      when {
        progress?.isFinished == 1L || fraction == 1.0 ->
          MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED
        fraction > 0.0 -> MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED
        else -> MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED
      }
    return Bundle().apply {
      putInt(MediaConstants.EXTRAS_KEY_COMPLETION_STATUS, status)
      if (status == MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED) {
        putDouble(MediaConstants.EXTRAS_KEY_COMPLETION_PERCENTAGE, fraction)
      }
    }
  }
}
