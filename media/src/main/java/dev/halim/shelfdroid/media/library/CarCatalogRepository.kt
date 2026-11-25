package dev.halim.shelfdroid.media.library

import android.content.Context
import android.database.ContentObserver
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.core.data.auth.AuthStateRepository
import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.data.library.LibraryRepository
import dev.halim.shelfdroid.core.data.library.PodcastEpisodeRepository
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.media.R
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext

internal class CarCatalogRepository
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val libraries: LibraryRepository,
  private val auth: AuthStateRepository,
  private val sync: LibraryDataRepository,
  private val libraryItems: LibraryItemRepository,
  private val podcastEpisodes: PodcastEpisodeRepository,
  private val progress: ProgressRepository,
  private val downloads: DownloadRepo,
  private val mapper: CarMediaItemMapper,
) {
  suspend fun startRefreshIfStale(): Job? = sync.startSynchronizationIfStale()

  suspend fun listChildren(parent: CarMediaId): List<MediaItem>? =
    withContext(Dispatchers.IO) {
      val token = auth.token.first()
      if (token.isBlank()) return@withContext emptyPublicTab(parent)
      val children =
        when (parent) {
          CarMediaId.Downloads ->
            listOf(
              mapper.folder(CarMediaId.DownloadedBooks),
              mapper.folder(CarMediaId.DownloadedEpisodes),
            )
          else -> {
            val tree =
              tree(if (parent is CarMediaId.Group) parent.parent else parent)
                ?: return@withContext null
            val rows =
              if (parent is CarMediaId.Group) tree.children(parent) ?: return@withContext null
              else tree.rows
            rows.mapNotNull { row ->
              val id = row.id
              if (id is CarMediaId.Group) mapper.group(id, row.title) else localItem(id)
            }
          }
        }
      if (auth.token.first() == token) children else emptyPublicTab(parent)
    }

  suspend fun findItem(id: CarMediaId): MediaItem? =
    withContext(Dispatchers.IO) {
      val token = auth.token.first()
      if (token.isBlank()) return@withContext null
      val item = localItem(id)
      item.takeIf { auth.token.first() == token }
    }

  fun observeChanges(parent: CarMediaId): Flow<Unit> {
    val catalogChanges =
      combine(
        auth.token.distinctUntilChanged(),
        libraries.observeLibraries(),
        libraryItems.observeItemChanges(),
        podcastEpisodes.observeEpisodeChanges(),
        progress.observeProgressChanges(),
      ) { _, _, _, _, _ ->
        Unit
      }
    return when (if (parent is CarMediaId.Group) parent.parent else parent) {
      CarMediaId.Downloads,
      CarMediaId.DownloadedBooks,
      CarMediaId.DownloadedEpisodes ->
        merge(catalogChanges, downloads.durableDownloads.map { Unit }, storageChanges()).conflate()
      else -> catalogChanges.conflate()
    }
  }

  private fun storageChanges(): Flow<Unit> = callbackFlow {
    val resolver = context.contentResolver
    val observer =
      object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
          downloads.refreshDurableDownloads()
          trySend(Unit)
        }
      }
    resolver.registerContentObserver(MediaStore.Downloads.EXTERNAL_CONTENT_URI, true, observer)
    resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
    downloads.refreshDurableDownloads()
    awaitClose { resolver.unregisterContentObserver(observer) }
  }

  private fun emptyPublicTab(parent: CarMediaId): List<MediaItem>? =
    when (parent) {
      CarMediaId.Books,
      CarMediaId.Podcasts,
      CarMediaId.Downloads -> emptyList()
      else -> null
    }

  private suspend fun localItem(id: CarMediaId): MediaItem? =
    when (id) {
      CarMediaId.ContinueListening -> null
      is CarMediaId.Folder -> mapper.folder(id)
      is CarMediaId.Library -> libraries.byId(id.libraryId)?.let(mapper::library)
      is CarMediaId.Group -> tree(id.parent)?.item(id)?.let { mapper.group(id, it.title) }
      is CarMediaId.Book ->
        catalogItem(id.bookId, isBook = true)?.let {
          mapper.libraryItem(it, progress.bookById(it.id))
        }
      is CarMediaId.Podcast ->
        catalogItem(id.podcastId, isBook = false)?.let { mapper.libraryItem(it) }
      is CarMediaId.Episode -> {
        val parent = catalogItem(id.podcastId, isBook = false)
        val episode = podcastEpisodes.findCachedEpisode(id.episodeId)
        if (parent != null && episode?.libraryItemId == parent.id) {
          mapper.episode(parent, episode, progress.episodeById(id.episodeId))
        } else null
      }
      is CarMediaId.LegacyPlayable -> legacyItem(id)
    }

  private suspend fun legacyItem(id: CarMediaId.LegacyPlayable): MediaItem? {
    val (libraryItemId, secondaryId) = id.id
    val record = libraryItems.byId(libraryItemId) ?: return null
    val canonicalId =
      if (record.isBook == 1L) {
        if (
          secondaryId != null &&
            libraryItems.bookById(libraryItemId)?.audioTracks?.none {
              it.index.toString() == secondaryId
            } != false
        )
          return null
        CarMediaId.Book(libraryItemId)
      } else {
        CarMediaId.Episode(libraryItemId, secondaryId ?: return null)
      }
    return localItem(canonicalId)?.buildUpon()?.setMediaId(id.value)?.build()
  }

  private suspend fun tree(parent: CarMediaId): CarBrowseTree? {
    val (rows, order) =
      when (parent) {
        CarMediaId.Books,
        CarMediaId.Podcasts -> {
          val matching =
            libraries.listLibraries().filter {
              it.isBookLibrary == if (parent == CarMediaId.Books) 1L else 0L
            }
          when (matching.size) {
            0 -> emptyList<CarBrowseRow>() to CarBrowseTree.Order.LIBRARY
            1 -> libraryRows(matching.single()) to CarBrowseTree.Order.TITLE
            else ->
              matching.map { CarBrowseRow(CarMediaId.Library(it.id), it.name) } to
                CarBrowseTree.Order.LIBRARY
          }
        }
        is CarMediaId.Library -> {
          val library = libraries.byId(parent.libraryId) ?: return null
          libraryRows(library) to CarBrowseTree.Order.TITLE
        }
        is CarMediaId.Podcast -> {
          catalogItem(parent.podcastId, isBook = false) ?: return null
          podcastEpisodes.listBrowseEpisodes(parent.podcastId).map {
            CarBrowseRow(CarMediaId.Episode(parent.podcastId, it.id), it.title, it.publishedAt)
          } to CarBrowseTree.Order.EPISODE_DATE
        }
        CarMediaId.DownloadedBooks -> downloadedBooks() to CarBrowseTree.Order.TITLE
        CarMediaId.DownloadedEpisodes -> downloadedEpisodes() to CarBrowseTree.Order.EPISODE_DATE
        else -> return null
      }
    return CarBrowseTree(parent, rows, order, context.getString(R.string.car_unknown_episode_date))
  }

  private fun libraryRows(library: LibraryEntity): List<CarBrowseRow> =
    libraryItems
      .listBrowseItems(library.id)
      .filter { it.isBook == library.isBookLibrary }
      .map { record ->
        val id =
          if (record.isBook == 1L) CarMediaId.Book(record.id) else CarMediaId.Podcast(record.id)
        CarBrowseRow(id, record.title)
      }

  private fun catalogItem(id: String, isBook: Boolean): LibraryItemEntity? {
    val record = libraryItems.byId(id) ?: return null
    val type = if (isBook) 1L else 0L
    if (record.isBook != type) return null
    val library = libraries.byId(record.libraryId)
    return record.takeIf { library?.isBookLibrary == type }
  }

  private suspend fun downloadedBooks(): List<CarBrowseRow> =
    rowsByLibraryType(1L).filter { row ->
      val id = (row.id as CarMediaId.Book).bookId
      val book = libraryItems.bookById(id) ?: return@filter false
      val record = catalogItem(id, isBook = true) ?: return@filter false
      downloads.isBookDownloaded(record.title, record.author, book.audioTracks)
    }

  private suspend fun downloadedEpisodes(): List<CarBrowseRow> =
    rowsByLibraryType(0L).flatMap { row ->
      val id = (row.id as CarMediaId.Podcast).podcastId
      val cached = podcastEpisodes.byLibraryItemId(id)
      val downloaded = downloads.podcastDownloadedEpisodeIds(row.title, cached)
      cached
        .filter { it.id in downloaded }
        .map {
          CarBrowseRow(CarMediaId.Episode(id, it.id), it.title, it.publishedAt)
        }
    }

  private fun rowsByLibraryType(type: Long): List<CarBrowseRow> =
    libraries.listLibraries().filter { it.isBookLibrary == type }.flatMap(::libraryRows)
}
