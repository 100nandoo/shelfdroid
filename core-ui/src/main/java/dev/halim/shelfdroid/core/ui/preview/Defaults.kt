package dev.halim.shelfdroid.core.ui.preview

import dev.halim.shelfdroid.core.LogLevel
import dev.halim.shelfdroid.core.PlayPauseControlState
import dev.halim.shelfdroid.core.PlayerBookmark
import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.PodcastFolder
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisode
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeDownloadState
import dev.halim.shelfdroid.core.data.screen.apikeys.ApiKeyUi
import dev.halim.shelfdroid.core.data.screen.apikeys.ApiKeysUiState
import dev.halim.shelfdroid.core.data.screen.apikeys.createedit.CreateEditApiKeysUiState
import dev.halim.shelfdroid.core.data.screen.backups.BackupsUiState
import dev.halim.shelfdroid.core.data.screen.edititem.ChapterRow
import dev.halim.shelfdroid.core.data.screen.edititem.CoverSearchState
import dev.halim.shelfdroid.core.data.screen.edititem.DEFAULT_BOOK_MATCH_PROVIDER
import dev.halim.shelfdroid.core.data.screen.edititem.DEFAULT_PODCAST_MATCH_PROVIDER
import dev.halim.shelfdroid.core.data.screen.edititem.DetailsForm
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemMediaKind
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.data.screen.edititem.EpisodeRow
import dev.halim.shelfdroid.core.data.screen.edititem.EpisodeUpdateState
import dev.halim.shelfdroid.core.data.screen.edititem.LibraryFileRow
import dev.halim.shelfdroid.core.data.screen.edititem.MatchProvider
import dev.halim.shelfdroid.core.data.screen.edititem.MatchResultRow
import dev.halim.shelfdroid.core.data.screen.edititem.MatchState
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchResultRow
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastScheduleForm
import dev.halim.shelfdroid.core.data.screen.edititem.SeriesEntry
import dev.halim.shelfdroid.core.data.screen.edititem.schedule.PodcastScheduleMode
import dev.halim.shelfdroid.core.data.screen.edititem.schedule.PodcastScheduleSimpleBuilder
import dev.halim.shelfdroid.core.data.screen.edititem.schedule.PodcastScheduleSimpleInterval
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.home.LibraryUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import dev.halim.shelfdroid.core.data.screen.logs.LogsUiState
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.data.screen.rssfeeds.RssFeedsUiState
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchPodcastUi
import dev.halim.shelfdroid.core.data.screen.userinfo.UserInfoUiState
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsUiState

object Defaults {
  const val USERNAME = "testuser"
  const val VERSION = "0.2.2"
  const val IMAGE_URL = ""
  const val TITLE = "Studio Dispatch"
  const val AUTHOR_NAME = "Mara Lee"
  const val DESCRIPTION =
    """
        Studio Dispatch is a fictional preview podcast used for ShelfDroid screenshots and UI fixtures.
        Each episode follows a small mobile team as they ship offline playback, clean up sync issues,
        and make release pipelines easier to trust on real devices.
        The copy is intentionally project-owned placeholder text so preview assets stay easy to
        redistribute and review.
    """

  // Home
  val HOME_BOOKS =
    listOf(
      BookUiState(id = BOOK_ID, author = BOOK_AUTHOR, title = BOOK_TITLE, cover = BOOK_COVER),
      BookUiState(
        id = "book2",
        author = "Nadia Rowan",
        title = "Tides of Winter",
        cover = BOOK_COVER,
      ),
      BookUiState(
        id = "book3",
        author = "Leo Mercer",
        title = "The Glass Orchard",
        cover = BOOK_COVER,
      ),
    )

  val HOME_PODCASTS =
    listOf(
      PodcastUiState(
        id = "podcast1",
        author = AUTHOR_NAME,
        title = TITLE,
        cover = IMAGE_URL,
        episodeCount = 5,
        unfinishedCount = 3,
        downloadedCount = 2,
        unfinishedAndDownloadCount = 1,
      ),
      PodcastUiState(
        id = "podcast2",
        author = AUTHOR_NAME,
        title = TITLE,
        cover = IMAGE_URL,
        episodeCount = 5,
        unfinishedCount = 3,
        downloadedCount = 2,
        unfinishedAndDownloadCount = 1,
      ),
      PodcastUiState(
        id = "podcast3",
        author = AUTHOR_NAME,
        title = TITLE,
        cover = IMAGE_URL,
        episodeCount = 7,
        unfinishedCount = 3,
        downloadedCount = 2,
        unfinishedAndDownloadCount = 2,
      ),
    )

  val HOME_LIBRARY_STATE =
    listOf(
      LibraryUiState(id = "1", name = "My Books", isBookLibrary = true, books = HOME_BOOKS),
      LibraryUiState(
        id = "2",
        name = "My Podcasts",
        isBookLibrary = false,
        podcasts = HOME_PODCASTS,
      ),
    )
  val HOME_UI_STATE =
    HomeUiState(state = GenericState.Success, librariesUiState = HOME_LIBRARY_STATE)
  val HOME_UI_STATE_LIST =
    HomeUiState(
      state = GenericState.Success,
      prefs = Prefs(),
      librariesUiState = HOME_LIBRARY_STATE,
    )

  // Book
  const val BOOK_ID = "1234567890"
  const val BOOK_AUTHOR = "Iris Vale"
  const val BOOK_TITLE = "Signal in the Harbor"
  const val BOOK_COVER = "https://unsplash.com/photos/brown-rock-formation-9qgKQewttVs"
  const val PROGRESS: Float = 0.5f
  const val PROGRESS_PERCENT = 12
  const val PROGRESS_COMPLETE_PERCENT = 100
  const val BOOK_DESCRIPTION =
    """
        Harbor mechanic Nera Quinn starts hearing coded signals in the fog sirens that ring across
        the breakwater each night. What begins as a maintenance job turns into a hunt for a missing
        research vessel, a buried map room, and a transmission that should have gone silent years ago.
        Signal in the Harbor is original placeholder copy used for ShelfDroid preview surfaces and
        release-listing screenshots.
    """
  const val BOOK_SUBTITLE = "Harbor Cycle, Book 1"
  const val BOOK_DURATION = "10 hour 20 minutes"
  const val BOOK_REMAINING = "2 hours 45 minutes"
  const val BOOK_NARRATOR = "Noah Park"
  const val BOOK_PUBLISH_YEAR = "2024"
  const val BOOK_PUBLISHER = "North Pier Audio"
  const val BOOK_GENRES = "Speculative fiction, Adventure"
  const val BOOK_LANGUAGE = "English"

  // Episode
  const val EPISODE_ID = "223344"
  const val EPISODE_TITLE =
    "Episode 4: Release Notes, Offline Queues, and Background Sync on Real Devices"
  const val EPISODE_PODCAST = "Studio Dispatch"
  const val EPISODE_PUBLISHED_AT = "19 June 2025"
  const val EPISODE_DESCRIPTION =
    """<p>This ShelfDroid preview episode follows a release week where the team tightens tag discipline, checks reproducible artifacts, and fixes an offline queue that behaved differently on weak hotel Wi-Fi.</p><p>It is original placeholder copy used for screenshots and Compose previews so release-listing assets stay easy to redistribute.</p>"""
  val EPISODES =
    listOf(
      Episode(
        "1",
        "Episode 1: Welcome to the Queue",
        "10 January 2024",
        0.75f,
        false,
        playPause = PlayPauseControlState(enabled = true, isPlaying = true, showPlayIcon = false),
      ),
      Episode("2", "Episode 2: Downloads on Trains", "30 May 2024", 0.5f, false),
      Episode("3", "Episode 3: Cleaning Up Sync Drift", "10 October 2024", 0.2f, false),
      Episode("4", EPISODE_TITLE, "25 April 2025", 0.0f, false),
      Episode("5", "Episode 5: Search, Caching, and Small Wins", "8 May 2025", 1.0f, true),
    )

  // PlayerChapter
  val DEFAULT_PLAYER_CHAPTER =
    PlayerChapter(
      id = 1,
      startTimeSeconds = 0.0,
      endTimeSeconds = 10.0,
      title = "Chapter 1: The Unexpected and Very Long Journey",
      startFormattedTime = "00:00",
      endFormattedTime = "00:10",
    )
  val DEFAULT_PLAYER_CHAPTER_LIST =
    listOf(
      DEFAULT_PLAYER_CHAPTER,
      PlayerChapter(
        id = 2,
        startTimeSeconds = 10.0,
        endTimeSeconds = 20.0,
        title = "Chapter 2: Whispers of the Past",
        startFormattedTime = "00:10",
        endFormattedTime = "00:20",
      ),
      PlayerChapter(
        id = 3,
        startTimeSeconds = 20.0,
        endTimeSeconds = 30.0,
        title = "Chapter 3: The Council of Elrond",
        startFormattedTime = "00:20",
        endFormattedTime = "00:30",
      ),
    )

  val MANY_PLAYER_CHAPTERS_LIST =
    (1..25).map {
      PlayerChapter(
        id = it,
        startTimeSeconds = ((it - 1) * 5).toDouble(),
        endTimeSeconds = (it * 5).toDouble(),
        title = "Chapter $it: A Long Title to Test Ellipsis and Wrapping Behavior in UI Components",
        startFormattedTime = String.format("%02d:%02d", (it - 1) * 5 / 60, (it - 1) * 5 % 60),
        endFormattedTime = String.format("%02d:%02d", it * 5 / 60, it * 5 % 60),
      )
    }

  val DEFAULT_PLAYER_BOOKMARK =
    PlayerBookmark("A very long bookmark title that should be truncated", "01:23", 83)

  val DEFAULT_PLAYER_BOOKMARK_LIST =
    listOf(
      DEFAULT_PLAYER_BOOKMARK,
      PlayerBookmark("Another bookmark", "02:34", 154),
      PlayerBookmark("Yet another bookmark", "03:45", 225),
    )

  val DEFAULT_PODCAST_FOLDER = PodcastFolder(id = "1", path = "/podcast")
  val DEFAULT_PODCAST_FOLDER_2 = PodcastFolder(id = "2", path = "/other-podcast")

  val SEARCH_PODCAST_1 =
    SearchPodcastUi(
      itunesId = 1,
      title = "The Joe Rogan Experience in Life",
      author = "Joe Rogan",
      genre = "Comedy, Podcasts, Entertainment",
      episodeCount = 2000,
      explicit = true,
      isAdded = true,
    )
  val SEARCH_PODCAST_2 =
    SearchPodcastUi(
      itunesId = 2,
      title = "Podcast with A Very Long Title That Will Overflow",
      author = "Joe Rogan",
      genre = "Comedy, Podcasts, Entertainment",
      episodeCount = 2000,
      explicit = true,
      isAdded = false,
    )

  val SEARCH_PODCAST_3 =
    SearchPodcastUi(
      itunesId = 3,
      title = "Podcast with A Very Long Title That Will Overflow",
      author = "Joe Rogan",
      genre = "Comedy, Podcasts, Entertainment",
      episodeCount = 2000,
      explicit = false,
      isAdded = true,
    )
  val SEARCH_PODCAST_4 =
    SearchPodcastUi(
      itunesId = 4,
      title = "Podcast with A Very Long Title That Will Overflow",
      author = "Joe Rogan",
      genre = "Comedy, Podcasts, Entertainment",
      episodeCount = 2000,
      explicit = false,
      isAdded = false,
    )

  val ADD_EPISODE_EPISODES =
    (1..10).map {
      val state =
        if (it % 2 == 0) AddEpisodeDownloadState.ToBeDownloaded
        else if (it % 5 == 0) AddEpisodeDownloadState.Downloaded
        else AddEpisodeDownloadState.NotDownloaded
      AddEpisode(
        episodeId = it.toString(),
        title = "Episode $it",
        description = "Description of Episode $it",
        publishedDate = "1 January 2023",
        publishedAt = 1672531200000,
        url = "https://example.com/episode$it",
        state = state,
      )
    }

  val LISTENING_SESSION_DEVICE_ANDROID =
    ListeningSessionUiState.Device(
      device = "Samsung S25 Ultra",
      client = "Shelfdroid 0.2.9",
      browser = null,
      ip = "192.168.1.24",
    )
  val LISTENING_SESSION_DEVICE_WEB =
    ListeningSessionUiState.Device(
      device = "Mac OS 10.15",
      client = "Abs Web 2.32.1",
      browser = "Firefox 147",
      ip = "192.168.1.3",
    )

  val LISTENING_SESSION: ListeningSessionUiState.Session =
    ListeningSessionUiState.Session(
      id = "1",
      item =
        ListeningSessionUiState.Item(
          author = "Brandon Sanderson",
          title = "The Way of Kings",
          narrator = "Michael Kramer",
          cover = "",
        ),
      device = LISTENING_SESSION_DEVICE_ANDROID,
      sessionTime =
        ListeningSessionUiState.SessionTime(
          duration = "30s",
          currentTime = 750.0,
          startedAt = "3 January 2026 09:00AM",
          updatedAt = "3 January 2026 09:01AM",
          startTime = "8:30:00",
          lastTime = "8:30:30",
          timeRange = "10.00–11.00 AM, 30 January 2026",
        ),
      user = ListeningSessionUiState.User(id = "user_12345", username = "Mark Webber Stephen"),
      playerInfo =
        ListeningSessionUiState.PlayerInfo(player = "media3_1.9.2", method = "Direct Play"),
    )

  val LISTENING_SESSIONS =
    listOf(LISTENING_SESSION, LISTENING_SESSION.copy(id = "2"), LISTENING_SESSION.copy(id = "3"))

  val USER_SETTINGS_LAST_SESSION =
    UserSettingsUiState.LastSession(
      title = "The Way of Kings",
      timeRange = "10.00–11.00 AM, 30 January 2026",
    )
  val USER_SETTINGS_USER_ADMIN =
    UserSettingsUiState.User(
      id = "1",
      username = "Brown",
      type = UserType.Admin,
      lastSeen = "36 minutes ago",
      isActive = true,
      lastSession = USER_SETTINGS_LAST_SESSION,
    )

  val USER_SETTINGS_USER_ROOT =
    UserSettingsUiState.User(
      id = "0",
      username = "admin",
      type = UserType.Root,
      lastSeen = "about 1 hour ago",
      isActive = true,
      lastSession = USER_SETTINGS_LAST_SESSION,
    )

  val USER_SETTINGS_USERS = listOf(USER_SETTINGS_USER_ROOT, USER_SETTINGS_USER_ADMIN)

  val USER_SETTINGS_UI_STATE = UserSettingsUiState(users = USER_SETTINGS_USERS)

  val API_KEYS =
    listOf(
      ApiKeyUi(
        id = "1",
        userId = "user-1",
        name = "Mobile Client",
        owner = "admin",
        expiresAt = "31 December 2026 11:59PM",
        lastUsedAt = "24 March 2026 6:45AM",
        isExpired = false,
        isActive = true,
      ),
      ApiKeyUi(
        id = "2",
        userId = "user-2",
        name = "Tablet Reader",
        owner = "root",
        expiresAt = "1 June 2026 8:00AM",
        lastUsedAt = null,
        isExpired = true,
        isActive = false,
      ),
    )

  val API_KEYS_UI_STATE = ApiKeysUiState(state = GenericState.Success, apiKeys = API_KEYS)

  val USER_INFO_MEDIA_PROGRESS =
    UserInfoUiState.MediaProgress(
      id = "3",
      title = "Running on Empty",
      cover = "",
      progress = "13%",
      startAt = "3 days ago",
      lastUpdate = "20 Jan 2026",
    )

  val USER_INFO_UI_STATE =
    UserInfoUiState(
      today = "2 hour 3 minutes",
      totalTime = "1 day 1 hour 1 minute 1 second\n",
      thisWeek = UserInfoUiState.ThisWeek(),
      mediaProgress =
        listOf(
          USER_INFO_MEDIA_PROGRESS,
          USER_INFO_MEDIA_PROGRESS.copy(id = "2", title = "The Way of Kings"),
        ),
    )

  val LOG_LOG =
    LogsUiState.LogItem.Log(
      1,
      LogLevel.WARNING,
      "[PodcastManager] runEpisodeCheck: \"The Diary Of A CEO with Steven Bartlett\" | Last check: Sat Mar 07 2026 08:30:18 GMT+0800 (Singapore Standard Time) | Latest episode pubDate: Fri Mar 06 2026 14:00:00 GMT+0800 (Singapore Standard Time)",
      "07:58:30",
    )

  val LOG_HOUR_HEADER = LogsUiState.LogItem.HourHeader(2, "11 PM")

  val LOG_UI_STATE =
    LogsUiState(
      state = GenericState.Success,
      logs = listOf(LOG_HOUR_HEADER, LOG_LOG, LOG_LOG.copy(id = 2)),
    )

  val BACKUPS_UI_STATE =
    BackupsUiState(
      state = GenericState.Success,
      backupLocation = "/metadata/backups",
      autoBackupEnabled = true,
      backupSchedule = "10 2 * * *",
      nextBackupDate = "28 March 2026 2:10AM",
      backupsToKeep = 2,
      maxBackupSize = 1,
      backups =
        listOf(
          BackupsUiState.BackupItem(
            id = "2026-03-27T1839",
            filename = "2026-03-27T1839.audiobookshelf",
            fileSize = "1.35 MB",
            createdAt = "27 March 2026 6:39PM",
            serverVersion = "2.33.1",
            downloadUrl = "",
          ),
          BackupsUiState.BackupItem(
            id = "2026-03-27T1834",
            filename = "2026-03-27T1834.audiobookshelf",
            fileSize = "1.35 MB",
            createdAt = "27 March 2026 6:34PM",
            serverVersion = "2.33.1",
            downloadUrl = "",
          ),
        ),
    )

  val RSS_FEED_EPISODES =
    listOf(
      RssFeedsUiState.EpisodeUi(
        id = "episode-1",
        title = "Most Replayed Moment",
        publishedAtText = "26 June 2026 1:00PM",
      ),
      RssFeedsUiState.EpisodeUi(
        id = "episode-2",
        title = "An Episode With A Longer Title To Exercise Wrapping In The Sheet",
        publishedAtText = "24 June 2026 9:30AM",
      ),
    )

  val RSS_FEED =
    RssFeedsUiState.RssFeedUi(
      id = "feed-1",
      title = "The Diary of a CEO",
      slug = "doac",
      entityType = "libraryItem",
      episodeCount = 6,
      preventIndexing = true,
      updatedAtText = "8 July 2026 10:37AM",
      publicFeedUrl = "https://audio.example.com/feed/doac",
      coverUrl = "",
      ownerName = "Cross",
      ownerEmail = "cross@example.com",
      episodes = RSS_FEED_EPISODES,
    )

  val RSS_FEEDS_UI_STATE = RssFeedsUiState(state = GenericState.Success, feeds = listOf(RSS_FEED))

  // Edit Item
  val EDIT_ITEM_DETAILS_FORM =
    DetailsForm(
      title = BOOK_TITLE,
      subtitle = BOOK_SUBTITLE,
      authors = listOf(BOOK_AUTHOR),
      narrators = listOf(BOOK_NARRATOR),
      series = listOf(SeriesEntry("The Lord of the Rings", "1")),
      genres = listOf("Fantasy", "Adventure"),
      tags = listOf("epic", "classic"),
      publishedYear = BOOK_PUBLISH_YEAR,
      publisher = BOOK_PUBLISHER,
      description = BOOK_DESCRIPTION.trimIndent(),
      isbn = "978-0261103573",
      asin = "B007978NPG",
      language = BOOK_LANGUAGE,
      explicit = false,
      abridged = false,
    )

  val EDIT_ITEM_PODCAST_DETAILS_FORM =
    DetailsForm(
      title = "The Daily Stack",
      genres = listOf("Technology", "News"),
      tags = listOf("mobile", "android"),
      description = "A daily podcast about mobile engineering.",
      language = "en",
      explicit = true,
      podcastAuthor = "Ana Silva",
      rssFeedUrl = "https://example.com/feed.xml",
      releaseDate = "2026-06-17",
      itunesId = "123456789",
      podcastType = "serial",
    )

  val EDIT_ITEM_CHAPTERS =
    listOf(
      ChapterRow(id = 1, title = "A Long-expected Party", start = 0.0, end = 1832.0),
      ChapterRow(id = 2, title = "The Shadow of the Past", start = 1832.0, end = 4105.0),
      ChapterRow(id = 3, title = "Three is Company", start = 4105.0, end = 6240.0),
    )

  val EDIT_ITEM_FILES =
    listOf(
      LibraryFileRow(
        ino = "1",
        path = "/audiobooks/The Fellowship of the Ring/fellowship.m4b",
        filename = "fellowship.m4b",
        sizeText = "524.29 MB",
        fileType = "audio/mp4",
      ),
      LibraryFileRow(
        ino = "2",
        path = "/audiobooks/The Fellowship of the Ring/cover.jpg",
        filename = "cover.jpg",
        sizeText = "245.00 KB",
        fileType = "image/jpeg",
      ),
    )

  val EDIT_ITEM_MATCH_PROVIDERS =
    listOf(
      MatchProvider(value = "google", text = "Google Books"),
      MatchProvider(value = "openlibrary", text = "Open Library"),
      MatchProvider(value = "audible", text = "Audible"),
    )

  val EDIT_ITEM_MATCH_RESULTS =
    listOf(
      MatchResultRow(
        cover = "",
        title = "The Fellowship of the Ring",
        author = "J. R. R. Tolkien",
        description = "The first volume of The Lord of the Rings.",
      ),
      MatchResultRow(
        cover = "",
        title = "The Two Towers",
        author = "J. R. R. Tolkien",
        description = "The second volume of The Lord of the Rings.",
      ),
    )

  val EDIT_ITEM_PODCAST_MATCH_RESULTS =
    listOf(
      PodcastMatchResultRow(
        cover = BOOK_COVER,
        title = "Android Developers Backstage",
        author = "Google",
        genres = listOf("Technology", "Software"),
        episodeCount = 212,
        feedUrl = "https://example.com/feed.xml",
        itunesId = "123456789",
        releaseDate = "2026-06-10",
        explicit = false,
        description = "Conversations with the Android team.",
      )
    )

  val EDIT_ITEM_UI_STATE =
    EditItemUiState(
      state = GenericState.Success,
      itemId = BOOK_ID,
      mediaKind = EditItemMediaKind.Book,
      coverUrl = BOOK_COVER,
      webBaseUrl = "http://localhost:13378",
      details = EDIT_ITEM_DETAILS_FORM,
      originalDetails = EDIT_ITEM_DETAILS_FORM,
      chapters = EDIT_ITEM_CHAPTERS,
      libraryFiles = EDIT_ITEM_FILES,
      match =
        MatchState.Book(
          providers = EDIT_ITEM_MATCH_PROVIDERS,
          selectedProvider = DEFAULT_BOOK_MATCH_PROVIDER,
          title = BOOK_TITLE,
          author = BOOK_AUTHOR,
          results = EDIT_ITEM_MATCH_RESULTS,
        ),
      coverSearch =
        CoverSearchState(
          providers = EDIT_ITEM_MATCH_PROVIDERS,
          title = BOOK_TITLE,
          author = BOOK_AUTHOR,
        ),
    )

  val EDIT_ITEM_PODCAST_UI_STATE =
    EditItemUiState(
      state = GenericState.Success,
      itemId = "podcast-id",
      mediaKind = EditItemMediaKind.Podcast,
      coverUrl = BOOK_COVER,
      webBaseUrl = "http://localhost:13378",
      details = EDIT_ITEM_PODCAST_DETAILS_FORM,
      originalDetails = EDIT_ITEM_PODCAST_DETAILS_FORM,
      schedule =
        PodcastScheduleForm(
          autoDownloadEpisodes = true,
          cronExpression = "15 23 * * *",
          maxEpisodesToKeepInput = "0",
          maxNewEpisodesToDownloadInput = "3",
        ),
      originalSchedule =
        PodcastScheduleForm(
          autoDownloadEpisodes = true,
          cronExpression = "15 23 * * *",
          maxEpisodesToKeepInput = "0",
          maxNewEpisodesToDownloadInput = "3",
        ),
      scheduleMode = PodcastScheduleMode.Simple,
      simpleScheduleBuilder =
        PodcastScheduleSimpleBuilder(
          interval = PodcastScheduleSimpleInterval.Daily,
          selectedHour = "23",
          selectedMinute = "15",
        ),
      episodes =
        listOf(
          EpisodeRow(
            id = "episode-1",
            title = "Episode 10: Moving from WebViews to Custom Tabs",
            secondaryText = "48 minutes ∙ 72.10 MB",
          ),
          EpisodeRow(
            id = "episode-2",
            title = "Episode 9: Now in Android at Google I/O",
            secondaryText = "42 minutes",
          ),
          EpisodeRow(
            id = "episode-3",
            title = "Episode 8: Storage changes in Android 16",
          ),
        ),
      episodeUpdate =
        EpisodeUpdateState(
          persistedCutoffMillis = 1750253400000,
          selectedCutoffMillis = 1750253400000,
          limitInput = "3",
        ),
      match =
        MatchState.Podcast(
          providers = listOf(MatchProvider(value = "itunes", text = "Apple Podcasts")),
          selectedProvider = DEFAULT_PODCAST_MATCH_PROVIDER,
          searchTerm = EDIT_ITEM_PODCAST_DETAILS_FORM.title,
          hasSearched = true,
          results = EDIT_ITEM_PODCAST_MATCH_RESULTS,
        ),
      coverSearch =
        CoverSearchState(
          providers = listOf(MatchProvider(value = "itunes", text = "Apple Podcasts")),
          title = EDIT_ITEM_PODCAST_DETAILS_FORM.title,
          author = EDIT_ITEM_PODCAST_DETAILS_FORM.podcastAuthor,
        ),
    )

  val EDIT_API_KEYS_UI_STATE_ACTIVE =
    CreateEditApiKeysUiState(
      state = GenericState.Success,
      apiKeyId = "123",
      name = "asdf",
      isActive = true,
    )
  val EDIT_API_KEYS_UI_STATE_INACTIVE =
    CreateEditApiKeysUiState(
      state = GenericState.Success,
      apiKeyId = "124",
      name = "qwer",
      isActive = false,
    )
}
