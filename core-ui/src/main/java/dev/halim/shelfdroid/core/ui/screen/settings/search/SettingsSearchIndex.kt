package dev.halim.shelfdroid.core.ui.screen.settings.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.navigation.SettingsSearchDestination

data class SettingsSearchEntry(
  val id: String,
  val destination: SettingsSearchDestination,
  val title: String,
  val path: List<String>,
  val keywords: String = "",
  val aliases: List<String> = emptyList(),
)

/** Offline search over setting labels and curated metadata. Preference values are never indexed. */
class SettingsSearchIndex(entries: List<SettingsSearchEntry>) {
  private data class IndexedEntry(
    val entry: SettingsSearchEntry,
    val title: String,
    val titleWords: List<String>,
    val keywordWords: List<String>,
    val pathWords: List<String>,
    val aliases: List<String>,
  )

  private val indexed = entries.map { entry ->
    val title = normalize(entry.title)
    IndexedEntry(
      entry = entry,
      title = title,
      titleWords = words(title),
      keywordWords = words(normalize(entry.keywords + " " + entry.aliases.joinToString(" "))),
      pathWords = words(normalize(entry.path.joinToString(" "))),
      aliases = entry.aliases.map(::normalize),
    )
  }

  fun search(query: String): List<SettingsSearchEntry> {
    val normalized = normalize(query)
    val terms = words(normalized).distinct()
    if (terms.isEmpty()) return emptyList()

    return indexed
      .mapNotNull { indexedEntry ->
        var score = 0
        for (term in terms) {
          score +=
            when {
              term in indexedEntry.titleWords -> 100
              indexedEntry.titleWords.any { it.startsWith(term) } -> 80
              term in indexedEntry.keywordWords -> 60
              indexedEntry.keywordWords.any { it.startsWith(term) } -> 50
              indexedEntry.pathWords.any { it.startsWith(term) } -> 20
              else -> return@mapNotNull null
            }
        }
        score +=
          when {
            indexedEntry.title == normalized || normalized in indexedEntry.aliases -> 1000
            indexedEntry.title.startsWith(normalized) -> 500
            indexedEntry.title.contains(normalized) -> 200
            else -> 0
          }
        indexedEntry.entry to score
      }
      .sortedByDescending { it.second }
      .map { it.first }
  }

  private fun normalize(value: String): String =
    buildString(value.length) {
      var pendingSpace = false
      for (character in value.lowercase()) {
        if (character.isLetterOrDigit()) {
          if (pendingSpace) append(' ')
          append(character)
          pendingSpace = false
        } else {
          pendingSpace = isNotEmpty()
        }
      }
    }

  private fun words(value: String): List<String> = value.split(' ').filter(String::isNotEmpty)
}

object SettingsSearchSettingId {
  const val DARK_MODE = "display.dark_mode"
  const val DYNAMIC_THEME = "display.dynamic_theme"
  const val LIST_VIEW = "home.list_view"
  const val SHOW_ONLY_DOWNLOADED = "home.show_only_downloaded"
  const val PERMANENT_DELETE = "home.permanent_delete"
  const val BOOK_SORT = "home.book_sort"
  const val BOOK_ORDER = "home.book_order"
  const val PODCAST_SORT = "home.podcast_sort"
  const val PODCAST_ORDER = "home.podcast_order"
  const val SEEK_BACK_INTERVAL = "player.seek_back_interval"
  const val SEEK_FORWARD_INTERVAL = "player.seek_forward_interval"
  const val CHAPTER_TIME_DISPLAY = "player.chapter_time_display"
  const val CHAPTER_TITLE_MAX_LINES = "player.chapter_title_max_lines"
  const val KEEP_SPEED_BETWEEN_TYPES = "playback.keep_speed_between_types"
  const val KEEP_TIMER_BETWEEN_TYPES = "playback.keep_timer_between_types"
  const val KEEP_SPEED_BETWEEN_EPISODES = "playback.keep_speed_between_episodes"
  const val KEEP_TIMER_BETWEEN_EPISODES = "playback.keep_timer_between_episodes"
  const val KEEP_SPEED_BETWEEN_BOOKS = "playback.keep_speed_between_books"
  const val KEEP_TIMER_BETWEEN_BOOKS = "playback.keep_timer_between_books"
  const val NOTIFICATION_OPENING_SCREEN = "notification.opening_screen"
  const val NOTIFICATION_PLAYER_PRESENTATION = "notification.player_presentation"
  const val SLEEP_TIMER_MODE = "notification.sleep_timer_mode"
  const val DEFAULT_SLEEP_TIMER = "notification.default_sleep_timer"
  const val SLEEP_TIMER_CYCLE = "notification.sleep_timer_cycle"
  const val PLAYBACK_SPEED_MODE = "notification.playback_speed_mode"
  const val PLAYBACK_SPEED_TARGET = "notification.playback_speed_target"
  const val PLAYBACK_SPEED_CYCLE = "notification.playback_speed_cycle"
  const val NOTIFICATION_ACTION_ONE = "notification.action_one"
  const val NOTIFICATION_ACTION_TWO = "notification.action_two"
  const val PODCAST_PERMANENT_DELETE = "podcast.permanent_delete"
  const val AUTO_SELECT_FINISHED_EPISODES = "podcast.auto_select_finished_episodes"
  const val HIDE_DOWNLOADED_EPISODES = "podcast.hide_downloaded_episodes"
}

@Composable
fun rememberSettingsSearchIndex(
  canAccessPodcastSettings: Boolean,
  canManagePodcastSettings: Boolean,
): SettingsSearchIndex {
  val settings = stringResource(R.string.settings)
  val display = stringResource(R.string.display)
  val home = stringResource(R.string.home_screen)
  val player = stringResource(R.string.player)
  val playback = stringResource(R.string.playback)
  val notification = stringResource(R.string.notification)
  val podcast = stringResource(R.string.podcast)
  val bookLibrary = stringResource(R.string.book_library)
  val podcastLibrary = stringResource(R.string.podcast_library)
  val switchingTypes = stringResource(R.string.when_switching_between_book_and_podcast)
  val switchingEpisodes = stringResource(R.string.when_switching_between_podcast_episodes)
  val switchingBooks = stringResource(R.string.when_switching_between_books)

  val entries = buildList {
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.DARK_MODE,
        SettingsSearchDestination.SETTINGS,
        stringResource(R.string.dark_mode),
        listOf(settings, display),
        keywords = "appearance theme night",
        aliases = listOf("dark", "dark theme"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.DYNAMIC_THEME,
        SettingsSearchDestination.SETTINGS,
        stringResource(R.string.dynamic_theme),
        listOf(settings, display),
        keywords = "appearance wallpaper color",
        aliases = listOf("dynamic colors", "wallpaper colors"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.LIST_VIEW,
        SettingsSearchDestination.HOME,
        stringResource(R.string.list_view),
        listOf(settings, home),
        keywords = "layout catalog books podcasts",
        aliases = listOf("list layout"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.SHOW_ONLY_DOWNLOADED,
        SettingsSearchDestination.HOME,
        stringResource(R.string.show_only_downloaded),
        listOf(settings, home),
        keywords = "offline filter downloads",
        aliases = listOf("downloaded filter", "offline only"),
      )
    )
    if (canAccessPodcastSettings) {
      add(
        SettingsSearchEntry(
          SettingsSearchSettingId.PERMANENT_DELETE,
          SettingsSearchDestination.HOME,
          stringResource(R.string.user_permanent_delete),
          listOf(settings, home),
          keywords = "delete books podcasts",
          aliases = listOf("hard delete", "permanently delete"),
        )
      )
    }
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.BOOK_SORT,
        SettingsSearchDestination.HOME,
        stringResource(R.string.sort),
        listOf(settings, home, bookLibrary),
        keywords = "books title author order",
        aliases = listOf("book sorting", "sort books"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.BOOK_ORDER,
        SettingsSearchDestination.HOME,
        stringResource(R.string.order),
        listOf(settings, home, bookLibrary),
        keywords = "books ascending descending",
        aliases = listOf("book sort order", "sort direction"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.PODCAST_SORT,
        SettingsSearchDestination.HOME,
        stringResource(R.string.sort),
        listOf(settings, home, podcastLibrary),
        keywords = "podcasts title author date",
        aliases = listOf("podcast sorting", "sort podcasts"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.PODCAST_ORDER,
        SettingsSearchDestination.HOME,
        stringResource(R.string.order),
        listOf(settings, home, podcastLibrary),
        keywords = "podcasts ascending descending",
        aliases = listOf("podcast sort order", "sort direction"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.SEEK_BACK_INTERVAL,
        SettingsSearchDestination.PLAYER,
        stringResource(R.string.seek_back_interval),
        listOf(settings, player, stringResource(R.string.seek)),
        keywords = "rewind seconds time",
        aliases = listOf("backward seek", "skip back"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.SEEK_FORWARD_INTERVAL,
        SettingsSearchDestination.PLAYER,
        stringResource(R.string.seek_forward_interval),
        listOf(settings, player, stringResource(R.string.seek)),
        keywords = "forward seconds time",
        aliases = listOf("skip forward", "fast forward"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.CHAPTER_TIME_DISPLAY,
        SettingsSearchDestination.PLAYER,
        stringResource(R.string.chapter_time_display),
        listOf(settings, player, stringResource(R.string.chapter)),
        keywords = "duration timestamp format",
        aliases = listOf("chapter time format", "time range"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.CHAPTER_TITLE_MAX_LINES,
        SettingsSearchDestination.PLAYER,
        stringResource(R.string.chapter_title_max_lines),
        listOf(settings, player, stringResource(R.string.chapter)),
        keywords = "text rows lines",
        aliases = listOf("chapter title lines", "chapter title length"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.KEEP_SPEED_BETWEEN_TYPES,
        SettingsSearchDestination.PLAYBACK,
        stringResource(R.string.keep_playback_speed),
        listOf(settings, playback, switchingTypes),
        keywords = "remember retain speed",
        aliases = listOf("keep speed"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.KEEP_TIMER_BETWEEN_TYPES,
        SettingsSearchDestination.PLAYBACK,
        stringResource(R.string.keep_sleep_timer),
        listOf(settings, playback, switchingTypes),
        keywords = "remember retain timer",
        aliases = listOf("keep timer", "preserve sleep timer"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.KEEP_SPEED_BETWEEN_EPISODES,
        SettingsSearchDestination.PLAYBACK,
        stringResource(R.string.keep_playback_speed),
        listOf(settings, playback, switchingEpisodes),
        keywords = "remember retain speed podcast episode",
        aliases = listOf("keep episode speed"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.KEEP_TIMER_BETWEEN_EPISODES,
        SettingsSearchDestination.PLAYBACK,
        stringResource(R.string.keep_sleep_timer),
        listOf(settings, playback, switchingEpisodes),
        keywords = "remember retain timer podcast episode",
        aliases = listOf("keep episode timer"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.KEEP_SPEED_BETWEEN_BOOKS,
        SettingsSearchDestination.PLAYBACK,
        stringResource(R.string.keep_playback_speed),
        listOf(settings, playback, switchingBooks),
        keywords = "remember retain speed audiobook",
        aliases = listOf("keep book speed"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.KEEP_TIMER_BETWEEN_BOOKS,
        SettingsSearchDestination.PLAYBACK,
        stringResource(R.string.keep_sleep_timer),
        listOf(settings, playback, switchingBooks),
        keywords = "remember retain timer audiobook",
        aliases = listOf("keep book timer"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.NOTIFICATION_OPENING_SCREEN,
        SettingsSearchDestination.NOTIFICATION,
        stringResource(R.string.media_notification_opening_screen),
        listOf(settings, notification),
        keywords = "tap opens app screen",
        aliases = listOf("notification tap destination", "open app from notification"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.NOTIFICATION_PLAYER_PRESENTATION,
        SettingsSearchDestination.NOTIFICATION,
        stringResource(R.string.media_notification_player_presentation),
        listOf(settings, notification),
        keywords = "tap player expanded mini",
        aliases = listOf("notification player size", "mini player expanded player"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.SLEEP_TIMER_MODE,
        SettingsSearchDestination.NOTIFICATION,
        stringResource(R.string.sleep_timer_notification_mode),
        listOf(settings, notification, stringResource(R.string.sleep_timer)),
        keywords = "toggle cycle cyclical repeat",
        aliases = listOf("timer action", "sleep timer behavior"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.DEFAULT_SLEEP_TIMER,
        SettingsSearchDestination.NOTIFICATION,
        stringResource(R.string.default_sleep_timer),
        listOf(settings, notification, stringResource(R.string.sleep_timer)),
        keywords = "duration minutes preset",
        aliases = listOf("sleep timer duration", "timer preset"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.SLEEP_TIMER_CYCLE,
        SettingsSearchDestination.NOTIFICATION,
        stringResource(R.string.sleep_timer_cycle),
        listOf(settings, notification, stringResource(R.string.sleep_timer)),
        keywords = "preset sequence order",
        aliases = listOf("timer presets", "cycle timer durations"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.PLAYBACK_SPEED_MODE,
        SettingsSearchDestination.NOTIFICATION,
        stringResource(R.string.playback_speed_notification_mode),
        listOf(settings, notification, stringResource(R.string.playback_speed)),
        keywords = "toggle cycle cyclical repeat",
        aliases = listOf("speed action", "speed button behavior"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.PLAYBACK_SPEED_TARGET,
        SettingsSearchDestination.NOTIFICATION,
        stringResource(R.string.playback_speed_toggle_target),
        listOf(settings, notification, stringResource(R.string.playback_speed)),
        keywords = "target speed multiplier",
        aliases = listOf("toggle playback speed", "speed toggle value"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.PLAYBACK_SPEED_CYCLE,
        SettingsSearchDestination.NOTIFICATION,
        stringResource(R.string.playback_speed_cycle),
        listOf(settings, notification, stringResource(R.string.playback_speed)),
        keywords = "preset sequence order",
        aliases = listOf("speed presets", "cycle playback speeds"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.NOTIFICATION_ACTION_ONE,
        SettingsSearchDestination.NOTIFICATION,
        stringResource(R.string.media_notification_button_1),
        listOf(settings, notification, stringResource(R.string.media_notification_buttons)),
        keywords = "first action control",
        aliases = listOf("notification button one"),
      )
    )
    add(
      SettingsSearchEntry(
        SettingsSearchSettingId.NOTIFICATION_ACTION_TWO,
        SettingsSearchDestination.NOTIFICATION,
        stringResource(R.string.media_notification_button_2),
        listOf(settings, notification, stringResource(R.string.media_notification_buttons)),
        keywords = "second action control",
        aliases = listOf("notification button two"),
      )
    )
    if (canAccessPodcastSettings) {
      add(
        SettingsSearchEntry(
          SettingsSearchSettingId.PODCAST_PERMANENT_DELETE,
          SettingsSearchDestination.PODCAST,
          stringResource(R.string.user_permanent_deletion),
          listOf(settings, podcast),
          keywords = "delete episode hard",
          aliases = listOf("permanently delete episodes"),
        )
      )
      add(
        SettingsSearchEntry(
          SettingsSearchSettingId.AUTO_SELECT_FINISHED_EPISODES,
          SettingsSearchDestination.PODCAST,
          stringResource(R.string.auto_select_finished_episodes),
          listOf(settings, podcast),
          keywords = "select completed played episodes",
          aliases = listOf("select played episodes automatically"),
        )
      )
    }
    if (canManagePodcastSettings) {
      add(
        SettingsSearchEntry(
          SettingsSearchSettingId.HIDE_DOWNLOADED_EPISODES,
          SettingsSearchDestination.PODCAST,
          stringResource(R.string.hide_downloaded_episodes),
          listOf(settings, podcast),
          keywords = "filter offline downloads",
          aliases = listOf("hide offline episodes"),
        )
      )
    }
  }

  return remember(entries) { SettingsSearchIndex(entries) }
}
