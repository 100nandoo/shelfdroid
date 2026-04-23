# Google Assistant Integration

## Overview

Enables Google Assistant voice control over ShelfDroid across four levels:

| Voice command | Capability |
|---|---|
| "Hey Google, pause/resume ShelfDroid" | Media Session (Part 1) |
| "Hey Google, play [book/episode] on ShelfDroid" | `actions.intent.PLAY_MEDIA` |
| "Hey Google, play latest episode from [podcast] on ShelfDroid" | `actions.intent.PLAY_MEDIA` (`podcast_name`) |
| "Hey Google, open [library/history/settings] on ShelfDroid" | `actions.intent.OPEN_APP_FEATURE` |
| "Hey Google, search [query] on ShelfDroid" | `actions.intent.SEARCH` |
| "Hey Google, open [book/podcast title] on ShelfDroid" | `actions.intent.GET_THING` |

---

## Part 1 — Basic Playback Control (Media Session)

`PlaybackService` already declares the correct `MediaLibraryService` and `MediaBrowserService` intent filters. Two missing methods are added to `MediaLibrarySession.Callback` in `PlayerModule.kt`:

- **`onGetLibraryRoot()`** — returns a valid root so Google Assistant recognises the app as a media provider.
- **`onAddMediaItems()`** — resolves incoming `MediaItem` requests from Google Assistant to playable items.

**Files changed:**
- `media/src/main/java/dev/halim/shelfdroid/media/di/PlayerModule.kt`
- `media/src/main/java/dev/halim/shelfdroid/media/service/PlayerStore.kt` (adds `resolveMediaItem()`)

---

## Part 2 — App Actions: Play, Search, Open Feature

Three App Actions capabilities are declared in `shortcuts.xml`:

- `actions.intent.PLAY_MEDIA` — triggers content search and playback by title.
- `actions.intent.SEARCH` — opens the library with a pre-filled search query.
- `actions.intent.OPEN_APP_FEATURE` — navigates to Library, History, or Settings via named feature shortcuts.

**Intent flow:** Google Assistant delivers an `ACTION_VIEW` intent to `MainActivity` with action-specific extras (`query`, `search_query`, `feature`). `MainActivity.handleExtra()` dispatches to the right `NavRequest`.

**Files changed:**
- `app/src/main/res/xml/shortcuts.xml` (created)
- `app/src/main/res/values/strings.xml`
- `app/src/main/AndroidManifest.xml`
- `core-database/.../LibraryItemEntity.sq` (adds `searchByTitle` SQL)
- `core-data/.../response/LibraryItemRepo.kt` (adds `searchByTitle()`)
- `core-ui/.../navigation/PendingMediaIdHandler.kt` (extends `NavRequest`)
- `core-ui/.../navigation/Navigation.kt` (feature routing)
- `core-ui/.../screen/MainActivity.kt`

---

## Part 3 — App Actions: Play Latest Podcast Episode

Extends `PLAY_MEDIA` with a `podcast_name` parameter. When triggered:

1. `LibraryItemRepo.latestEpisodeFromPodcast(name)` searches the local database for a matching podcast, deserialises its episodes from the stored JSON, and returns the `(libraryItemId, episodeId)` pair with the highest `publishedAt` timestamp.
2. `MainActivity` encodes these as a composite `mediaId` and sets `NavRequest(isOpenPlayer = true)`.

**Files changed:**
- `app/src/main/res/xml/shortcuts.xml` (extra parameter)
- `core-data/.../response/LibraryItemRepo.kt` (adds `latestEpisodeFromPodcast()`)
- `core-ui/.../screen/MainActivity.kt`

---

## Part 4 — App Actions: Open Specific Item Detail Page

`actions.intent.GET_THING` maps a spoken item name to a detail-screen navigation without starting playback.

1. `MainActivity` receives `item_name` extra.
2. Calls `searchByTitle()`, takes the first match.
3. Sets `NavRequest(mediaId = match.id, isOpenPlayer = false)` → routes to `BookScreen` or `PodcastScreen`.

**Files changed:**
- `app/src/main/res/xml/shortcuts.xml` (new capability)
- `core-ui/.../screen/MainActivity.kt`

---

## Verification

1. Install the app on a device linked to a Google account.
2. **Basic control:** Start playing, say "Hey Google, pause" / "Hey Google, resume ShelfDroid".
3. **Play by title:** "Hey Google, play [book title] on ShelfDroid" → plays the book.
4. **Latest episode:** "Hey Google, play latest episode from [podcast name] on ShelfDroid".
5. **Feature open:** "Hey Google, open my listening history on ShelfDroid" → opens ListeningSession screen.
6. **Search:** "Hey Google, search Harry Potter on ShelfDroid" → Home opens with query pre-filled.
7. **Detail page:** "Hey Google, open [title] on ShelfDroid" → opens detail screen without playing.
8. Use **Android Studio App Actions Test Tool** (`Tools > App Actions Test Tool`) to test all voice commands locally without publishing to Play Store.

---

## Notes

- App Actions require the app on the Play Store (or an internal test track) for full Google Assistant discovery. Use the App Actions Test Tool plugin during development.
- `actions.intent.PLAY_MEDIA` and other built-in intents need no additional SDK dependency — only `shortcuts.xml` declaration.
- Part 1 (basic media control) works on any installed device; no Play Store requirement.
- To test the App Actions Test Tool, install the **Google Assistant plugin** in Android Studio.
