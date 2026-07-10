## Unreleased

### Feat

- **RssFeeds**: open rss feed
- **RssFeedScreen**: full implementation
- **MiscScreen**: split into 2 section; server, client
- **HomeScreen**: handle empty state cause by filter
- **RefreshToken**: handle http code 401 for relogin flow
- **BookMatchTab**: add ASIN support for book match tab

### Fix

- **deps**: update hilt to v2.60.1 (#253)
- **deps**: update dependency androidx.test.uiautomator:uiautomator to v2.4.0 (#249)
- **deps**: update dependency androidx.hilt:hilt-navigation-compose to v1.4.0 (#248)
- **deps**: update dependency androidx.compose:compose-bom to v2026.06.01 (#247)
- **deps**: update androidxnavigation3 to v1.2.0-alpha05 (#246)

### Refactor

- **RssFeedScreen**: move dummy data to Defaults
- **EditEpisode**: code review changes
- **gitignore**: ignore codebase-memory
- **Prefs**: podcast default sort is progress, desc
- **BookMatchTab**: similar implementation with podcast
- **Checkbox**: whole row checkbox become toggleable
- **DetailsTab**: split between book and podcast details for better dx
- **BookMatchTab**: improve book match ui

## 0.4.7 (2026-06-25)

### Fix

- **LoginScreen**: improve loading
- **Podcast**: coverpath empty cause crash
- **LoginScreen**: improve server url validation
- **ScheduleTab**: code cleanup
- **AddEpisodeScreen**: improve ui
- **ScheduleTab**: ui improvement and run linter
- **deps**: update hilt to v2.60 (#242)
- **deps**: update androidxdatastore (#237)

## 0.4.6 (2026-06-23)

### Fix

- **versions**: fix datastore use wrong variable

## 0.4.5 (2026-06-23)

## 0.4.4 (2026-06-23)

## 0.4.3 (2026-06-22)

### Feat

- **Font**: remove downloadable font, use local font instead
- **CreateEditApiKeysScreen**: add copy to clipboard function
- **AddEpisodeScreen**: implement filter functionality
- **AddEpisodeScreen**: show publish at instead of description

### Fix

- **PodcastMatchReviewSheet**: `Apply selected` button should behave like the web
- **CoverTab**: upload cover not working
- **CoverTab**: podcast provider dropdown
- **deps**: update androidxcore to v1.19.0 (#197)
- **UserInfoScreen**: fix data not loaded and lint check
- **Button**: regression on a7e8110
- **HomeItemGrid**: add vertical layout wrapper

### Refactor

- **PodcastMatchReviewSheet**: improve ui
- **PodcastMatchTab**: improve viewmodel code
- **EpisodesTab**: ui and code improvement

## 0.4.2 (2026-06-07)

### Fix

- **DownloadRepo**: fix slow first start cause by book and podcast catalog

## 0.4.1 (2026-06-07)

### Feat

- **Download**: remove offline download recovery message
- **ChapterSheet**: add chapter time display options
- **EditItem**: improve ui
- **core-ui**: audit remaining preview gaps
- **core-ui**: preview overflow states
- **core-ui**: backfill simple compose previews
- **preview**: backfill stateful dialog coverage
- **ACRA**: add crash reporting
- **EditItemScreen**: wip, finished details tab
- **SettingsPlayer**: add chapter title max line prefs
- **Media-Notification**: add timer button

### Fix

- **PlayerModule**: use default data source instead of okhttpdatasource
- **deps**: update datetime to v0.8.0-0.6.x-compat (#194)
- **deps**: update dependency androidx.compose:compose-bom to v2026.05.01 (#187)
- **ChaptersTab**: add edit chapter button
- **FilesTab**: improve ui and functionality
- **EditItem**: flatten tools and cover tab
- **deps**: update kotlin datetime
- **deps**: update kotlinx-coroutines monorepo to v1.11.0 (#182)
- **deps**: update dependency androidx.compose:compose-bom to v2026.05.00 (#180)
- **deps**: update dependency org.jetbrains.kotlinx:kotlinx-datetime to v0.8.0-0.6.x-compat (#181)
- **deps**: update androidxmedia3 to v1.10.1 (#175)
- **deps**: update dependency com.google.android.material:material to v1.14.0 (#177)
- **deps**: update dependency androidx.navigation:navigation-compose to v2.9.8 (#176)
- **CoverTab**: improve search and update cover logic
- **deps**: update dependency org.jetbrains.kotlinx:kotlinx-serialization-json to v1.11.0 (#164)
- **BackupScreen**: add notification permission and ime padding
- **BackupScreen**: upload and download function
- **Player**: use media controller for play pause
- **deps**: update hilt to v2.59.2
- **Player**: play/pause not working after app sent to background for a while
- **MetadataSerializer**: improve logic to avoid crash

### Refactor

- **DownloadRepo**: download book handling
- **DownloadRepo**: improve download progress, title and content text
- **PlayerController**: use media controller to control exoplayer instead of player manager
- **Sort**: change book library sort to desc progress

## 0.4.0 (2026-03-31)

### Feat

- **ServerSettingsScreen**: finish ui and api integration
- **BackupsScreen**: almost finished, pending upload and download
- **ApiKeysScreen**: finish add api keys ui and api intergration
- **EditApiKeysScreen**: finish edit api key screen and api integration
- **ApiKeysScreen**: finish delete api key integration
- **ApiKeysScreen**: finish get api keys ui and api call
- **ApiKeys**: screen skeleton and api calls integration
- **LogsScreen**: add search filter with suggestions
- **LogsScreen**: add server log level and filter
- **LogsScreen**: make log more readable by adding some colors

### Fix

- **deps**: update androidxmedia3 to v1.10.0 (#159)
- **deps**: update dependency androidx.compose:compose-bom to v2026.03.01 (#158)
- **deps**: update sqldelight to v2.3.2 (#157)
- **deps**: update androidxmedia3 to v1.9.3 (#156)
- **deps**: update kotlin monorepo to v2.3.20 (#155)
- **UserType**: keep this enum on release build
- **deps**: update dependency androidx.datastore:datastore-preferences to v1.2.1 (#153)
- **deps**: update sqldelight to v2.3.1 (#154)

### Refactor

- **Icons**: migrate to xml instead of compose icons
- **PlayerViewModel**: refactor to singleton and rename to `PlayerController`

## 0.3.0 (2026-03-12)

### Feat

- **ChangePasswordScreen**: add change password screen
- **UserInfoScreen**: add saved media progress
- **ListeningStat**: stat calculation and ui, pending: handle caculation for zero values
- **ListeningStatScreen**: add screen skeleton, api call, localdb
- **UserSettingsScreen**: add create user flow
- **EditUserScreen**: add form validation
- **MiscScreen**: show button depend on login user type
- **UserSettingsScreen**: add delete user functionality
- **UserSettingsEditUserScreen**: add library access setting
- **UserSettingsEditUserScreen**: finish handle tags
- **UserSettingsEditUserScreen**: wip dropdown textfield for tags and libraries
- **UserSettingsUpdateUser**: api call for update user
- **UserSettingsEditUserScreen**: edit user screen
- **MiscScreen**: add button to navigate some managements page
- **OpenSession**: add open session screen
- **ListeningSession**: add multiple delete functionality
- **ListeningSessionSheet**: add player info and play method helper
- **ListeningSessionItem**: add detail bottom sheet on click
- **SettingsListeningSessionScreen**: add setting screen for listening sessions and save users on sqldelight
- **ListeningSession**: add user filter
- **SegmentedButton**: add segment button component
- **ListeningSession**: add items per page prefs
- **ListeningSessions**: change page implementation
- **SettingsScreen**: fix padding
- **ListeningSession**: add user
- **ListeningSession**: listening session page still wip
- **PodcastScreen**: add auto select finished episode delete behaviour
- **SettingsPodcastScreen**: add episode hide downloaded switch
- **AddEpisodeScreen**: finish filter dialog implementation
- **AddEpisodeScreen**: wip filter dialog

### Fix

- **deps**: update dependency androidx.core:core-ktx to v1.18.0 (#149)
- **deps**: update dependency androidx.activity:activity-compose to v1.13.0 (#147)
- **deps**: update dependency androidx.compose:compose-bom to v2026.03.00 (#148)
- **deps**: update dependency org.jetbrains.kotlinx:kotlinx-serialization-json to v1.10.0
- **deps**: update kotlin monorepo to v2.3.10
- **deps**: update dependency androidx.compose:compose-bom to v2026.02.01
- **deps**: update dependency io.coil-kt.coil3:coil-compose to v3.4.0
- **deps**: update dependency androidx.activity:activity-compose to v1.12.4
- **DropdownOutlinedTextField**: on option removed bug fix
- **deps**: update androidxmedia3 to v1.9.2
- **deps**: update dependency androidx.compose:compose-bom to v2026.01.01
- **deps**: update dependency androidx.navigation:navigation-compose to v2.9.7
- **deps**: update dependency androidx.activity:activity-compose to v1.12.3
- **deps**: update androidxmedia3 to v1.9.1
- **Mix**: various bugfixes

### Refactor

- **UserInfoScreen**: rename `ListeningStatScreen` to `UserInfoScreen`
- **TextLabelValue**: use accordingly
- **Events**: use sealed interface instead of class for event
- **EditUserScreen**: rename from UserSettingsEditUserScreen
- **UserSettingsScreen**: make uistate reactive
- **Long**: use proper long to boolean extension function
- **Extensions**: use proper snackbar extension functions
- **ListeningSessionScreen**: keep item click function consistent
- **ListeningSession**: improve ui state to reduce recomposition, fix system bar padding

## 0.2.9 (2026-01-20)

### Feat

- **SettingsScreen**: add podcast screen section
- **PodcastScreen**: hard delete for episode
- **PodcastScreen**: delete episode
- **SocketManager**: socket io initial implementation
- **PodcastScreen**: download episodes UI and api integration
- **Animation**: add predefined animated visibility composable
- **PodcastScreen**: alpha value per episode depends on completion
- **ProgressRow**: add completed progress
- **PodcastScreen**: add animation when navigating to `AddEpisodeScreen`
- **AddEpisodeScreen**: add download state and map accordingly
- **AddEpisodeScreen**: WIP is downloaded field logic
- **HomeScreen**: hard/soft delete functionality
- **SettingsScreen**: add re-login functionality

### Fix

- **deps**: update hilt to v2.58
- **deps**: update dependency io.socket:socket.io-client to v2.1.2
- **deps**: update dependency androidx.compose:compose-bom to v2026
- **PodcastFeed**: fix podcast feed api return null cause error
- **PodcastScreen**: episode title and podcast author animations
- **Media3**: `smallPlayer` play button not working after app sent to background
- **LoginScreen**: error handling for retrofit base url

### Refactor

- **PodcastFeedScreen**: rename to `AddPodcastScreen`
- **ContextReceiver**: remove usage
- **ItemCover**: move and rename to `Cover`
- **HomeScreen**: remove navigation using event and change to direct function parameter
- **SnackbarHost**: use centralized snackbarHost
- **ExoPlayer**: instance only come from manager not hilt

## 0.2.8 (2025-12-22)

### Feat

- **HomeScreen**: delete library item

### Fix

- **Podcast**: metadata and feed response that can be null
- **deps**: update dependency androidx.compose:compose-bom to v2025.12.01
- **deps**: update androidxmedia3 to v1.9.0
- **deps**: update dependency androidx.activity:activity-compose to v1.12.2

## 0.2.7 (2025-12-12)

### Feat

- **PodcastFeedScreen**: add snackbar when successfully create a new podcast
- **SettingsPlayback**: add book for playback behaviour
- **SettingsPlayback**: add playback behaviour settings
- **SearchPodcast**: update newly added podcast `isAdded` state
- **PodcastFeed**: finish create podcast
- **SearchPodcast**: add already in library indicator
- **HomeScreen**: only show add podcast button for admin
- **MediaItemMapper**: set artwork data from coil disk cache
- **PodcastFeed**: show form properly
- **PodcastFeed**: wip show feed form properly
- **SearchPodcastScreen**: search new podcast to be added
- **SettingsScreen**: add sort and sort order
- **HomeScreen**: seperate sort order between podcast and book library

### Fix

- **SettingsScreen**: fix sort not using enum label
- **deps**: update dependency androidx.activity:activity-compose to v1.12.1
- **deps**: update dependency androidx.compose:compose-bom to v2025.12.00
- **deps**: update dependency androidx.datastore:datastore-preferences to v1.2.0
- **deps**: update dependency androidx.core:core-splashscreen to v1.2.0
- **deps**: update sqldelight to v2.2.1
- **deps**: update dependency androidx.lifecycle:lifecycle-runtime-ktx to v2.10.0
- **deps**: update dependency androidx.activity:activity-compose to v1.12.0
- **deps**: update dependency androidx.navigation:navigation-compose to v2.9.6
- **deps**: update dependency androidx.compose:compose-bom to v2025.11.01
- **deps**: update dependency androidx.compose:compose-bom to v2025.10.00
- **HomeScreen**: remote data fetching blocking ui
- **deps**: update dependency androidx.compose:compose-bom to v2025.09.01
- **deps**: update dependency androidx.navigation:navigation-compose to v2.9.5
- **deps**: update hilt to v2.57.2
- **deps**: update androidxlifecycle to v2.9.4
- **HomeScreen**: book download filter

### Refactor

- **SettingsScreen**: change items ordering
- **NavPayload**: refactor payload being passed from search podcast screen to podcast feed screen
- **NavResult**: this file dedicated for navigation result parcel data class
- **SettingsScreen**: improve layout and arrangement for `HomeScreenSection`
- **HomeScreen**: make the data reactive using flow

## 0.2.6 (2025-09-18)

### Feat

- **HomeScreen**: add sort and filter
- **HomeScreen**: add animate item

### Fix

- **SessionManager**: still sync when playback is pause not from app ui button
- **SessionManager**: prevent duplicate sync on app resume
- **deps**: update dependency androidx.hilt:hilt-navigation-compose to v1.3.0
- **deps**: update dependency androidx.benchmark:benchmark-macro-junit4 to v1.4.1
- **deps**: update dependency androidx.compose:compose-bom to v2025.09.00
- **deps**: update dependency androidx.navigation:navigation-compose to v2.9.4
- **deps**: update dependency androidx.activity:activity-compose to v1.11.0
- **EpisodeItem**: toggle finished on non existance progress entity
- **EpisodeItem**: toggle finished on non existance progress entity

### Perf

- **Exoplayer**: use audio only renderer to reduce app size

## 0.2.5 (2025-08-30)

### Feat

- **HomeScreen**: make download state for books reactive
- **HomeScreen**: collect downloads stateflow to update the ui state
- **DownloadRepo**: implement cleanup when item is removed from the server
- **Downloaded-Filter**: save filter to datastore and filter podcast screen episode according to the filter
- **HomeScreen**: add download filter
- **HomeScreen**: make podcast episode unfinished count reactive
- **PlayerEventListener**: when podcast is finished auto mark it as finished on the progressEntity
- **Download**: refactor download state on some uiState classes
- **Download**: allow download book with multiple tracks
- **SleepTimer**: add clear option and update ui of bottom sheet
- **Player**: support multiple tracks book
- **Player**: allow to play multiple track
- **Download**: add warning dialog for download button
- **Notification**: add pending intent to completed and failed notification
- **Bookmark**: handle goto bookmark

### Fix

- **deps**: update dependency androidx.compose:compose-bom to v2025.08.01
- **deps**: update androidxlifecycle to v2.9.3
- **deps**: update hilt to v2.57.1
- **deps**: update dependency com.github.skydoves:retrofit-adapters-result to v1.1.0
- **deps**: update dependency androidx.compose:compose-bom to v2025.08.00
- **deps**: update dependency androidx.core:core-ktx to v1.17.0
- **LocalSync**: request using wrong play method, change the way timeListened being calculate

### Refactor

- **DownloadTracker**: combine with DownloadRepo
- **Helper**: move helper to it's own gradle module
- **Download**: create seperate gradle module for download
- **State**: use isbook from internal state
- **PlayerRepository**: migrate to internal state
- **Session-&-Timer**: improve how sync work and timer now use player manager instead of it's own listener

## 0.2.4 (2025-08-09)

### Feat

- **Notification**: add title to download notification
- **LocalSync**: podcast sync
- **LocalSync**: book sync for multiple entity
- **LocalSync**: wip book sync, pending sync if multiple entity exist
- **Download**: dont do remote sync on local playback
- **Download**: allow playback for downloaded item
- **Font**: use merriweather for title font
- **BigPlayer**: dragging slider now will show target position
- **HomeScreen**: add list view
- **SettingsScreen**: add list view
- **HomeScreen**: add unfinished episode count on podcast
- **Download**: allow download book with single track
- **Download**: download episode button on episode screen
- **RefreshToken**: invalidate refresh token on logout
- **Permission**: handle download notification permission for android 13
- **SessionManager**: skip sync on media item change
- **Logout**: restart app on logout

### Fix

- **PendingMediaIdHandler**: fix duplicate screen when media notification is clicked
- **Notification-Icon**: invert the vector
- **deps**: update dependency androidx.test.espresso:espresso-core to v3.7.0
- **deps**: update dependency androidx.test.ext:junit to v1.3.0
- **deps**: update dependency androidx.test:runner to v1.7.0
- **deps**: update dependency androidx.test:core to v1.7.0
- **deps**: update androidxmedia3 to v1.8.0
- **deps**: update dependency androidx.navigation:navigation-compose to v2.9.3
- **deps**: update dependency androidx.benchmark:benchmark-macro-junit4 to v1.4.0
- **PodcastScreen**: optimise podcast screen unnecessary recompose
- **SkipPreviousButton**: fix can't go to start of chapter or podcast
- **Download**: fix collecting stateflow on episode and book viewmodel
- **Download**: set custom cache key

### Refactor

- **PlayerInternalState**: move some of the field from player ui state to player internal state
- **Book-&-Podcast-Screen**: make it reactive using flow
- **Lifecycle**: use collectAsStateWithLifecycle instead of collectAsState
- **PlayerUiState**: move to core module
- **LoginScreen**: code cleanup

## 0.2.3 (2025-07-25)

### Feat

- **Download**: download podcast with progress
- **Download**: basic download podcast
- **BasicPlayerControl**: fix ripple and sizing issue cause by IconButton
- **SettingsScreen**: add username
- **API**: implement new jwt token flow
- **Media3**: add simple cache 100MB
- **Media3**: start media controller when user go to book or podcast screen
- **Database**: cleanup when entity is deleted
- **Player**: long interval sync session
- **Player**: add sync session with short interval
- **BigPlayer**: create bookmark
- **BigPlayer**: finished edit bookmark
- **Player**: delete bookmark
- **BigPlayer**: show bookmarks
- **BookmarkRepo**: retrieve and save bookmarks on homescreen
- **DeviceInfo**: update play request and device info
- **BigPlayer**: implement sleep timer
- **BigPlayer**: implement speed slider
- **PlayerUiState**: hold by service ui state holder instead, to survive lifecycle
- **DataStore**: implement generate and retrieve deviceid
- **Logout**: clear and stop playback when logout
- **PlayButton**: Handle play button state on book and episode screen
- **PodcastScreen**: handle play and progress state for currently played episode
- **PlayerRepository**: handle seek slider for book
- **Player**: add playback progress
- **Media3**: change notification icon and commands
- **Player**: set media item properly and handle media notification click
- **Media3**: add basic media library session and media controller implementation
- **EpisodeScreen**: add cover and publishedAt
- **Player**: handle previous next enabled state
- **Player**: go to previous and next chapter functionality
- **Player**: change chapter functionality
- **Player**: add chapter ui implementation
- **Player**: seek back and forward button state
- **Player**: handle play pause button state
- **ApiService**: add sync session api

### Fix

- **SettingsScreen**: userprefs not updated
- **deps**: update hilt to v2.57
- **deps**: update coil to v3.3.0
- **deps**: update dependency androidx.compose:compose-bom to v2025.07.00
- **deps**: update androidxlifecycle to v2.9.2
- **deps**: update dependency androidx.navigation:navigation-compose to v2.9.2
- **BigPlayer**: fix create bookmark wrong starttime
- **Player**: clip media item so it only start and end based on chapter
- **PlayerViewModel**: ui state not in the right state when activity finish but there is ongoing playback
- **deps**: update dependency androidx.navigation:navigation-compose to v2.9.1
- **Player**: auto change chapter when book only have one file
- **deps**: update dependency org.jetbrains.kotlinx:kotlinx-serialization-json to v1.9.0
- **deps**: update dependency org.jetbrains.kotlin:kotlin-gradle-plugin to v2.2.0
- **Media3**: fix command buttons order
- **PlaybackService**: stop process when task remove
- **deps**: update dependency androidx.compose:compose-bom to v2025.06.01
- **Preview**: cleanup for some screen and component
- **Button**: fix button padding

### Refactor

- **Strings**: using strings.xml
- **General**: code cleanup
- **Media**: move media3 code related from ui module to media module
- **Player**: remove rememberstate usage for exoplayer button, using ui state data class instead
- **State.kt**: button state move to player ui state
- **PlayerRepository**: move mapper function to seperate class
- **HtmlConverter**: remove this 3rd party library

### Perf

- **HomeScreen**: remove scroll listener & add crossfade to image
- **ExoPlayer**: lazily intantiated

## 0.2.2 (2025-06-19)

### Feat

- **Podcast**: play podcast episode api & click handler
- **EpisodeScreen**: wip pending play button
- **Podcast**: add toggle is finished functionality
- **PodcastScreen**: play, download, mark as finished buttons
- **BookScreen**: add progress row
- **PodcastScreen**: sort episode by `publishedAt`
- **ApiService**: add play api
- **.editorconfig**: add editor config file
- **SmallPlayer**: bind data to ui
- **PlayerViewModel**: move player related variable into viewmodel
- **BookScreen,-PodcastScreen**: use library item from local db
- **LibraryEntity**: save library to db
- **LibraryItemEntity**: save library item to db
- **SQLDelight**: remove room and use sqlDelight
- **BigPlayer**: add backhandler and handle click on small player
- **HomeScreen**: move header and setting into inside lazyColumn
- **BigPlayer**: integrate bigplayer to playerhandler
- **MiniPlayer**: change implementation using bottomsheet
- **Animation**: add animation for book and podcast screen
- **SharedElement**: add shared element and shared bound transition
- **MiniPlayer**: finish ui
- **MiniPlayer**: add mini player implementation
- **PlayerScreen**: only ui implementation, state is not handle yet

### Fix

- **PlayResponse**: fix crash calling play api, somehow can't be wrap with `Result` adapter on `ApiService`
- **HomeScreen**: show progress only if progress at least at 1%
- **BookScreen**: percentage progress
- **deps**: update dependency androidx.compose:compose-bom to v2025.06.00
- **deps**: update androidxlifecycle to v2.9.1
- **deps**: update dependency androidx.compose:compose-bom to v2025.05.01
- **deps**: update dependency androidx.datastore:datastore-preferences to v1.1.7
- **deps**: update retrofit monorepo to v3
- **deps**: update coil to v3.2.0 (#45)
- **deps**: update androidxlifecycle to v2.9.0 (#42)
- **deps**: update dependency androidx.compose:compose-bom to v2025.05.00 (#43)
- **deps**: update dependency androidx.navigation:navigation-compose to v2.9.0 (#44)
- **deps**: update dependency androidx.datastore:datastore-preferences to v1.1.6
- **deps**: update dependency androidx.datastore:datastore-preferences to v1.1.5
- **deps**: update dependency be.digitalia.compose.htmlconverter:htmlconverter to v1.0.4

### Refactor

- **core-data**: move repository to related folder
- **Player**: from bottomSheetScaffold to normal composable
- **MiniPlayerHandler**: extract the code for `MiniPlayer` from `Navigation` to `MiniPlayerHandler`

## 0.2.1 (2025-04-26)

### Feat

- **update_version.sh**: add github actions
- **Preview**: add preview for all screen
- **BookScreen**: WIP, pending chapters, audio tracks, library files
- **PodcastScreen**: wip episode progress
- **HomeScreen**: ui improvement
- **Launcher-Icon**: add monochrome icon
- **SettingsScreen**: add dynamic theme switch
- **SettingsScreen**: finish logout and nav to settings
- **SettingsScreen**: wip, logout and nav to settings not implemented
- **Splashscreen**: add androidx core splashscreen implementation
- **Theme**: setup theme, font, typography
- **HomeItem**: still missing mediaState
- **HomeScreen**: finish load libraries and items api
- **HomeScreen**: api call still giving error
- **CoreNetwork**: basic login api implementation
- **LoginScreen**: basic implementation
- **Datastore**: add baseUrl, token, darkMode
- **Datastore**: add datastore module
- **Signing**: setup release build signing config
- **Launcher-Icon**: change launcher icon

### Fix

- **various**: bug fixes
- **deps**: update androidxroom to v2.7.1
- **deps**: update dependency androidx.datastore:datastore-preferences to v1.1.5
- **deps**: update dependency androidx.compose:compose-bom to v2025.04.01
- **deps**: update hilt to v2.56.2
- **deps**: update dependencies
- **deps**: update kotlinx-coroutines monorepo to v1.10.2
- **deps**: update dependency org.jetbrains.kotlinx:kotlinx-serialization-json to v1.8.1
- **deps**: update dependency org.jetbrains.kotlinx:kotlinx-datetime to v0.6.2
- **deps**: update dependency androidx.compose:compose-bom to v2025.03.01
- **deps**: update dependency androidx.datastore:datastore-preferences to v1.1.4
- **deps**: update hilt to v2.56.1
- **deps**: update dependency androidx.compose:compose-bom to v2025.03.00
- **deps**: update hilt to v2.56
- **deps**: update dependency androidx.navigation:navigation-compose to v2.8.9
- **Retrofit**: add `Result` adapter
- **deps**: update dependency androidx.compose:compose-bom to v2025
- **deps**: update dependency androidx.activity:activity-compose to v1.10.1
- **deps**: update dependency androidx.navigation:navigation-compose to v2.8.8

### Refactor

- **General**: refactor some classes to improve code quality
