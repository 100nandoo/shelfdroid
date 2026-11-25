package dev.halim.shelfdroid.media.library

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.media.notification.MediaNotificationButtons.BACK_COMMAND_BUTTON
import dev.halim.shelfdroid.media.notification.MediaNotificationButtons.FORWARD_COMMAND_BUTTON
import dev.halim.shelfdroid.media.notification.MediaNotificationButtons.PLAYBACK_SPEED_BUTTON
import dev.halim.shelfdroid.media.notification.MediaNotificationButtons.SLEEP_TIMER_OFF_BUTTON
import dev.halim.shelfdroid.media.notification.mediaNotificationButtons
import dev.halim.shelfdroid.media.playback.PlayerStore
import dev.halim.shelfdroid.media.session.CUSTOM_BACK
import dev.halim.shelfdroid.media.session.CUSTOM_FORWARD
import dev.halim.shelfdroid.media.session.CUSTOM_NEXT_CHAPTER
import dev.halim.shelfdroid.media.session.CUSTOM_PLAYBACK_SPEED
import dev.halim.shelfdroid.media.session.CUSTOM_PREVIOUS_CHAPTER
import dev.halim.shelfdroid.media.session.CUSTOM_SLEEP_TIMER
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
internal class ShelfMediaLibraryCallback
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val playerStore: Lazy<PlayerStore>,
  private val catalog: Lazy<CarCatalogRepository>,
  private val mapper: CarMediaItemMapper,
) : MediaLibrarySession.Callback {

  private lateinit var scope: CoroutineScope
  private val rootLimits = mutableMapOf<MediaSession.ControllerInfo, Int>()
  private val subscriptions = mutableMapOf<Subscription, BrowseSubscription>()
  private val lastColdRefreshByTab = mutableMapOf<Subscription, Job>()

  fun attach(serviceScope: CoroutineScope) {
    scope = serviceScope
  }

  private val sessionCommands =
    MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
      .apply {
        listOf(
            BACK_COMMAND_BUTTON,
            FORWARD_COMMAND_BUTTON,
            SLEEP_TIMER_OFF_BUTTON,
            PLAYBACK_SPEED_BUTTON,
          )
          .forEach { commandButton ->
            commandButton.sessionCommand?.let { add(it) }
          }
        add(SessionCommand(CUSTOM_NEXT_CHAPTER, Bundle()))
        add(SessionCommand(CUSTOM_PREVIOUS_CHAPTER, Bundle()))
        remove(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH)
        remove(SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT)
      }
      .build()

  override fun onConnectAsync(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
  ): ListenableFuture<MediaSession.ConnectionResult> {
    val store = playerStore.get()
    val commandButtons =
      mediaNotificationButtons(
        context.getString(CoreR.string.next_chapter),
        store.uiState.value,
        store.isChapterTransitioning.value,
        store.uiState.value.advancedControl.sleepTimerLeft > Duration.ZERO,
        store.notificationPrefs.value,
        seekBackSeconds = store.playerPrefs.value.seekBackSeconds,
        seekForwardSeconds = store.playerPrefs.value.seekForwardSeconds,
        previousChapterDisplayName = context.getString(CoreR.string.previous_chapter),
      )
    return Futures.immediateFuture(
      MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
        .setAvailableSessionCommands(sessionCommands)
        .setCustomLayout(commandButtons)
        .setMediaButtonPreferences(commandButtons)
        .build()
    )
  }

  override fun onCustomCommand(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
    customCommand: SessionCommand,
    args: Bundle,
  ): ListenableFuture<SessionResult> {
    val resultCode =
      when (customCommand.customAction) {
        CUSTOM_BACK -> {
          session.player.seekBack()
          SessionResult.RESULT_SUCCESS
        }
        CUSTOM_FORWARD -> {
          session.player.seekForward()
          SessionResult.RESULT_SUCCESS
        }
        CUSTOM_SLEEP_TIMER -> {
          playerStore.get().sleepTimerFromMediaNotification()
          SessionResult.RESULT_SUCCESS
        }
        CUSTOM_NEXT_CHAPTER -> {
          if (playerStore.get().nextChapterFromMediaNotification()) {
            SessionResult.RESULT_SUCCESS
          } else {
            SessionResult.RESULT_ERROR_INVALID_STATE
          }
        }
        CUSTOM_PREVIOUS_CHAPTER -> {
          if (playerStore.get().previousChapterFromMediaNotification()) {
            SessionResult.RESULT_SUCCESS
          } else {
            SessionResult.RESULT_ERROR_INVALID_STATE
          }
        }
        CUSTOM_PLAYBACK_SPEED -> {
          if (playerStore.get().changeSpeedFromMediaNotification()) {
            SessionResult.RESULT_SUCCESS
          } else {
            SessionResult.RESULT_ERROR_INVALID_STATE
          }
        }
        else -> SessionResult.RESULT_ERROR_NOT_SUPPORTED
      }
    return Futures.immediateFuture(SessionResult(resultCode))
  }

  override fun onGetLibraryRoot(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    params: LibraryParams?,
  ): ListenableFuture<LibraryResult<MediaItem>> {
    rootLimits[browser] = rootLimit(params) ?: ROOT_TABS.size
    val extras =
      Bundle(params?.extras ?: Bundle.EMPTY).apply {
        putInt(
          MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
          MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
        )
        putInt(
          MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
          MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
        )
      }
    val rootParams =
      LibraryParams.Builder()
        .setExtras(extras)
        .setRecent(params?.isRecent == true)
        .setOffline(params?.isOffline == true)
        .setSuggested(params?.isSuggested == true)
        .build()
    return Futures.immediateFuture(LibraryResult.ofItem(mapper.folder(CarMediaId.Root), rootParams))
  }

  override fun onGetChildren(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    parentId: String,
    page: Int,
    pageSize: Int,
    params: LibraryParams?,
  ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
    if (page < 0 || pageSize < 1) return badId()
    val parent = CarMediaId.parse(parentId) ?: return badId()
    if (parent == CarMediaId.Root) {
      val limit = rootLimit(params) ?: rootLimits[browser] ?: ROOT_TABS.size
      val rows = ROOT_TABS.take(limit).map(mapper::folder)
      return Futures.immediateFuture(LibraryResult.ofItemList(pageOf(rows, page, pageSize), params))
    }

    return libraryFuture {
      val repository = catalog.get()
      var rows = repository.listChildren(parent)
      if (parent == CarMediaId.Books || parent == CarMediaId.Podcasts) {
        if (rows.isNullOrEmpty()) {
          val scheduledRefresh = scope.async { startRefresh(repository) }
          val shouldReloadRows =
            withTimeoutOrNull(COLD_WAIT_MS) {
              // A shared refresh may have finished since the first cache read.
              val refresh = scheduledRefresh.await() ?: return@withTimeoutOrNull true
              val tab = Subscription(browser, parentId)
              if (lastColdRefreshByTab.put(tab, refresh) === refresh) return@withTimeoutOrNull false
              refresh.join()
              true
            } ?: true
          if (shouldReloadRows) rows = repository.listChildren(parent)
        } else {
          scope.launch { startRefresh(repository) }
        }
      }
      if (rows == null) LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
      else
        LibraryResult.ofItemList(
          pageOf(rows, page, pageSize).onEach { grantArtwork(browser, it) },
          params,
        )
    }
  }

  override fun onGetItem(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    mediaId: String,
  ): ListenableFuture<LibraryResult<MediaItem>> {
    val id = CarMediaId.parse(mediaId) ?: return badId()
    if (id == CarMediaId.Root || id in ROOT_TABS) {
      return Futures.immediateFuture(
        LibraryResult.ofItem(mapper.folder(id as CarMediaId.Folder), null)
      )
    }
    return libraryFuture {
      val item = catalog.get().findItem(id)
      if (item == null) LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
      else LibraryResult.ofItem(item.also { grantArtwork(browser, it) }, null)
    }
  }

  override fun onSubscribe(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    parentId: String,
    params: LibraryParams?,
  ): ListenableFuture<LibraryResult<Void>> {
    val parent = CarMediaId.parse(parentId) ?: return badId()
    if (parent == CarMediaId.Root) return super.onSubscribe(session, browser, parentId, params)

    val key = Subscription(browser, parentId)
    val subscription = BrowseSubscription()
    subscriptions.put(key, subscription)?.observer?.cancel()
    return libraryFuture {
      try {
        if (subscriptions[key] !== subscription) return@libraryFuture LibraryResult.ofVoid(params)
        val repository = catalog.get()
        val loaded = runCatching { repository.listChildren(parent) }
        if (subscriptions[key] !== subscription) return@libraryFuture LibraryResult.ofVoid(params)
        val initial =
          loaded.getOrThrow()
            ?: return@libraryFuture LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        subscription.observer = scope.launch {
          var previous = initial
          try {
            repository.observeChanges(parent).collect {
              val rows = repository.listChildren(parent).orEmpty()
              if (!sameRows(previous, rows)) {
                previous = rows
                session.notifyChildrenChanged(browser, parentId, rows.size, params)
              }
            }
          } catch (error: Exception) {
            if (error is CancellationException) throw error
            Log.w(TAG, "Unable to observe browse list", error)
          }
        }
        if (browser.controllerVersion != MediaSession.ControllerInfo.LEGACY_CONTROLLER_VERSION) {
          session.notifyChildrenChanged(browser, parentId, initial.size, params)
        }
        LibraryResult.ofVoid(params)
      } finally {
        if (subscriptions[key] === subscription && subscription.observer == null) {
          subscriptions.remove(key)
        }
      }
    }
  }

  override fun onUnsubscribe(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    parentId: String,
  ): ListenableFuture<LibraryResult<Void>> {
    subscriptions.remove(Subscription(browser, parentId))?.observer?.cancel()
    return Futures.immediateFuture(LibraryResult.ofVoid())
  }

  override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
    rootLimits.remove(controller)
    lastColdRefreshByTab.keys.removeAll { it.browser == controller }
    subscriptions.keys
      .filter { it.browser == controller }
      .forEach {
        subscriptions.remove(it)?.observer?.cancel()
      }
  }

  private suspend fun startRefresh(repository: CarCatalogRepository): Job? =
    try {
      repository.startRefreshIfStale()
    } catch (error: Exception) {
      if (error is CancellationException) throw error
      Log.w(TAG, "Unable to refresh the Catalog", error)
      null
    }

  private fun grantArtwork(browser: MediaSession.ControllerInfo, item: MediaItem) {
    val uri = item.mediaMetadata.artworkUri ?: return
    if (
      uri.scheme != ContentResolver.SCHEME_CONTENT ||
        !browser.isPackageNameVerified ||
        browser.packageName == context.packageName
    )
      return
    try {
      context.grantUriPermission(browser.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (error: SecurityException) {
      Log.w(TAG, "Unable to grant car artwork access", error)
    }
  }

  private fun sameRows(previous: List<MediaItem>, next: List<MediaItem>): Boolean =
    previous == next &&
      previous.zip(next).all { (old, new) ->
        val oldExtras = old.mediaMetadata.extras
        val newExtras = new.mediaMetadata.extras
        oldExtras?.getInt(MediaConstants.EXTRAS_KEY_COMPLETION_STATUS) ==
          newExtras?.getInt(MediaConstants.EXTRAS_KEY_COMPLETION_STATUS) &&
          oldExtras?.getDouble(MediaConstants.EXTRAS_KEY_COMPLETION_PERCENTAGE) ==
            newExtras?.getDouble(MediaConstants.EXTRAS_KEY_COMPLETION_PERCENTAGE) &&
          oldExtras?.getLong(MediaConstants.EXTRAS_KEY_DOWNLOAD_STATUS) ==
            newExtras?.getLong(MediaConstants.EXTRAS_KEY_DOWNLOAD_STATUS)
      }

  private fun rootLimit(params: LibraryParams?): Int? =
    params
      ?.extras
      ?.takeIf { it.containsKey(ROOT_LIMIT_HINT) }
      ?.getInt(ROOT_LIMIT_HINT, ROOT_TABS.size)
      ?.coerceIn(0, ROOT_TABS.size)

  // The car requests an unpaged list. Other Media3 clients can request a page of that bounded list.
  private fun pageOf(rows: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
    val start = page.toLong() * pageSize
    if (start >= rows.size) return emptyList()
    return rows.subList(start.toInt(), (start + pageSize).coerceAtMost(rows.size.toLong()).toInt())
  }

  private fun <T : Any> libraryFuture(
    block: suspend () -> LibraryResult<T>
  ): ListenableFuture<LibraryResult<T>> = scope.future {
    try {
      block()
    } catch (error: Exception) {
      if (error is CancellationException) throw error
      Log.w(TAG, "Unable to browse the Catalog", error)
      LibraryResult.ofError<T>(SessionError.ERROR_IO)
    }
  }

  private fun <T : Any> badId(): ListenableFuture<LibraryResult<T>> =
    Futures.immediateFuture(LibraryResult.ofError<T>(SessionError.ERROR_BAD_VALUE))

  private data class Subscription(val browser: MediaSession.ControllerInfo, val parent: String)

  private class BrowseSubscription(var observer: Job? = null)

  private companion object {
    const val TAG = "ShelfMediaLibrary"
    const val COLD_WAIT_MS = 3_000L
    const val ROOT_LIMIT_HINT = "androidx.media.MediaBrowserCompat.Extras.KEY_ROOT_CHILDREN_LIMIT"
    val ROOT_TABS = listOf(CarMediaId.Books, CarMediaId.Podcasts, CarMediaId.Downloads)
  }
}
