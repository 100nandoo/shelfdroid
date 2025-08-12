## Unreleased

### Feat

- **Bookmark**: handle goto bookmark

### Fix

- **LocalSync**: request using wrong play method, change the way timeListened being calculate

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
