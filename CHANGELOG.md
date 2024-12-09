## 0.1.1 (2024-12-09)

### Fix

- **android.yaml**: fix trigger action when tag is push

## 0.1.0 (2024-12-09)

### Feat

- **PlayerScreen**: jump to bookmark when bookmark item click
- **PlayerScreen**: add bookmarks
- **UserStore**: change progress store to user store
- **PlayerScreen**: add chapters list and jump to specific chapter
- **PlayerScreen**: handle play next chapter when current chapter finished
- **PlayerScreen**: add change chapter functionality
- **Github-Actions**: add debug build and upload artifacts
- **HomeRepository**: implement repository for home screen
- **ProgressStore**: implement media progress store
- **PlayerScreen**: implement seekback and seekforward
- **Store**: create extensions for fetching from network and failback to cached
- **PlayerScreen**: show current time, total time, handle playback value
- **ItemStore**: add chapters
- **PlayerScreen**: playback speed implementation
- **StoreManager**: as parent of all store
- **ItemStore**: implement store for library item
- **LibraryStore**: implement store for library
- **Database**: integrate sqldelight library 📖
- **PlayerScreen**: basic player screen
- **SessionManager**: create session manager for handling sync media session
- **Api**: integrate play and sync
- **MediaLibrarySession.Callback**: add seek forward and backward 10 seconds
- **PlaybackService**: handle playback resumption

### Fix

- **Splash.xml**: fix splash icon not showing for api 29
- **Github-Actions**: fix sed
- **Github-Actions**: fix sed2
- **Github-Actions**: add main branch on commit
- **Release**: fix release build issue cause by type safe navigation
- **PlaybackService**: reorder media button for below android 13

### Refactor

- **Media.adroid.kt**: handle move to next chapter when current is finish to Media instead of PlayerViewModel
- **Timer**: time listened and sleep timer calculation move to `Timer` class
- **PlayerScreen**: update ui for timer so it has the same height as speed slider
- **PlayerScreen**: fix slider
- **Media**: code improvement
- **HomeLibraryItem**: add padding on the icon
- **LibraryStore**: move query on database to seperate class
- **Api**: change from flow to suspend function
- **Navigation-Route**: using kotlin type safety dsl
