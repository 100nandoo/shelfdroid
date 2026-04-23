# Glance Compose Widgets — Implementation Plan

## Context

ShelfDroid needs three home-screen widgets built with **Jetpack Glance 1.2.1** (already declared in `libs.versions.toml`, not yet wired up). The app uses Hilt, Media3, SQLDelight, Coil 3, DataStore, and Kotlin coroutines — all required data models and repositories are already in place. No widget code exists yet.

---

## Pre-work: Fix version catalog bug + add dependency

**File:** `gradle/libs.versions.toml`

```toml
# WRONG — references androidxDatastore version (1.1.7) instead of androidxGlance (1.2.1)
androidx-glance = { module = "androidx.glance:glance-material3", version.ref = "androidxDatastore" }

# FIX
androidx-glance = { module = "androidx.glance:glance-material3", version.ref = "androidxGlance" }
```

**File:** `app/build.gradle.kts`

```kotlin
dependencies {
    // add:
    implementation(libs.androidx.glance)
}
```

---

## Module strategy

All widget code lives inside the existing **`:app`** module (it already depends on every needed module). Package root: `dev.halim.shelfdroid.widget`. A dedicated `:widget` module can be extracted later if needed.

---

## Widget 1 — Playback Control

**Purpose:** Show now-playing info and transport controls (play/pause, ±30 s, sleep timer) on the home screen.

### Data source

| Field | Source |
|-------|--------|
| `title`, `author`, `cover` | `PlayerUiState` via `PlayerStore` (`StateFlow`) |
| Play / Pause state | `PlayerUiState.exoState` (`ExoState.Playing` / `ExoState.Pause`) |
| Sleep timer remaining | `PlayerUiState.advancedControl.sleepTimerLeft` (`Duration`) |
| Transport commands | `MediaController` bound to `PlaybackService` inside each `ActionCallback` |

### Files to create

| Path | Purpose |
|------|---------|
| `app/src/main/java/dev/halim/shelfdroid/widget/playback/PlaybackWidget.kt` | `GlanceAppWidget` — composable UI |
| `app/src/main/java/dev/halim/shelfdroid/widget/playback/PlaybackWidgetReceiver.kt` | `GlanceAppWidgetReceiver` |
| `app/src/main/java/dev/halim/shelfdroid/widget/playback/PlaybackWidgetWorker.kt` | `CoroutineWorker` — reads `PlayerStore`, calls `update()` |
| `app/src/main/java/dev/halim/shelfdroid/widget/playback/PlaybackActions.kt` | `ActionCallback` per button |
| `app/src/main/res/xml/playback_widget_info.xml` | `appwidget-provider` metadata XML |

### UI layout

```
Row {
    AsyncImage(cover)                 // Coil bitmap → ImageProvider
    Column {
        Text(title)
        Text(author)
        Row {
            Button(rewind 30s)
            Button(play / pause)      // icon swaps on exoState
            Button(fast-forward 30s)
            Button(sleep timer)       // shows "12 min" when active, clock icon otherwise
        }
    }
}
```

### Action callbacks

| Callback | Command |
|----------|---------|
| `PlayPauseAction` | `MediaController.play()` / `.pause()` |
| `RewindAction` | `MediaController.seekBack()` |
| `FastForwardAction` | `MediaController.seekForward()` |
| `SleepTimerAction` | Broadcast intent → `PlaybackService` toggles timer |

### Update strategy

- `PlaybackWidgetWorker` polls every **15 s** (minimum WorkManager interval) while playback is active.
- `PlaybackService` also calls `PlaybackWidget().update(context)` directly on each `PlayerUiState` change (no delay).

---

## Widget 2 — Book Remaining Time

**Purpose:** Show a specific book's cover, title, and "X h Y min remaining" on the home screen.

### Data source

| Field | Source |
|-------|--------|
| `title`, `cover` | `LibraryItemRepo.byId(libraryItemId)` |
| `currentTime`, `duration` | `ProgressRepo.bookById(libraryItemId)` → `ProgressEntity` |
| Remaining time | `duration - currentTime` (seconds) → formatted as `"Xh Ymin"` |
| Chosen book | Stored per-widget in `DataStore<Preferences>` via `PreferencesGlanceStateDefinition` |

### State key

```kotlin
val KEY_LIBRARY_ITEM_ID = stringPreferencesKey("library_item_id")
```

### Files to create

| Path | Purpose |
|------|---------|
| `app/src/main/java/dev/halim/shelfdroid/widget/book/BookRemainingWidget.kt` | `GlanceAppWidget` |
| `app/src/main/java/dev/halim/shelfdroid/widget/book/BookRemainingWidgetReceiver.kt` | `GlanceAppWidgetReceiver` |
| `app/src/main/java/dev/halim/shelfdroid/widget/book/BookRemainingWorker.kt` | Worker — fetches progress, updates Glance state |
| `app/src/main/java/dev/halim/shelfdroid/widget/book/BookRemainingConfigActivity.kt` | Config activity — book picker (plain Compose `Activity`) |
| `app/src/main/res/xml/book_remaining_widget_info.xml` | `appwidget-provider` XML with `android:configure` attribute |

### UI layout

```
Column {
    AsyncImage(cover)
    Text(title, maxLines = 2)
    Text("3 h 4 min remaining")   // caption style, grayed out
}
```

### Configuration flow

1. User adds widget → system launches `BookRemainingConfigActivity`
2. Activity shows list of books from `HomeRepository` (title + cover)
3. On selection, writes `libraryItemId` to `DataStore<Preferences>` for this `GlanceId`
4. Activity calls `BookRemainingWidget().update(context)` then finishes with `RESULT_OK`

### Update strategy

`BookRemainingWorker` periodic every **30 min** via WorkManager. Also triggered immediately after config activity completes.

---

## Widget 3 — Latest Podcast Episode

**Purpose:** Show the newest episode for a chosen podcast — title, episode name, publish date, and a Play button.

### Data source

| Field | Source |
|-------|--------|
| Podcast `title`, `cover` | `LibraryItemRepo.byId(libraryItemId)` |
| Latest episode | Podcast's `episodes` list sorted `publishedAt DESC` by `PodcastMapper` → `episodes[0]` |
| Episode `title`, `publishedAt` | `PodcastEpisode.title`, `PodcastEpisode.publishedAt` (Unix ms → formatted date) |
| Chosen podcast | Stored per-widget in `DataStore<Preferences>` |

### Files to create

| Path | Purpose |
|------|---------|
| `app/src/main/java/dev/halim/shelfdroid/widget/podcast/LatestEpisodeWidget.kt` | `GlanceAppWidget` |
| `app/src/main/java/dev/halim/shelfdroid/widget/podcast/LatestEpisodeWidgetReceiver.kt` | `GlanceAppWidgetReceiver` |
| `app/src/main/java/dev/halim/shelfdroid/widget/podcast/LatestEpisodeWorker.kt` | Worker — fetches latest episode, updates state |
| `app/src/main/java/dev/halim/shelfdroid/widget/podcast/LatestEpisodeConfigActivity.kt` | Config activity — podcast picker |
| `app/src/main/res/xml/latest_episode_widget_info.xml` | `appwidget-provider` XML with `android:configure` attribute |

### UI layout

```
Column {
    Row {
        AsyncImage(podcastCover)
        Text(podcastTitle)
    }
    Text(episodeTitle, maxLines = 2)
    Text("Apr 20, 2026")           // formatted from publishedAt
    Button("Play") → deep-link intent → opens app at this episode
}
```

### Update strategy

`LatestEpisodeWorker` periodic every **1 h**. Also runs on `onUpdate()` (system-triggered on boot / re-add).

---

## AndroidManifest additions (`app/src/main/AndroidManifest.xml`)

```xml
<!-- Widget 1: Playback Control -->
<receiver
    android:name=".widget.playback.PlaybackWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE"/>
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/playback_widget_info"/>
</receiver>

<!-- Widget 2: Book Remaining -->
<receiver
    android:name=".widget.book.BookRemainingWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE"/>
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/book_remaining_widget_info"/>
</receiver>
<activity
    android:name=".widget.book.BookRemainingConfigActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE"/>
    </intent-filter>
</activity>

<!-- Widget 3: Latest Podcast Episode -->
<receiver
    android:name=".widget.podcast.LatestEpisodeWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE"/>
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/latest_episode_widget_info"/>
</receiver>
<activity
    android:name=".widget.podcast.LatestEpisodeConfigActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE"/>
    </intent-filter>
</activity>
```

---

## Critical files to read before implementing

| File | Why |
|------|-----|
| `core/src/main/java/dev/halim/shelfdroid/core/PlayerUiState.kt` | Full playback state shape |
| `core-data/src/main/java/dev/halim/shelfdroid/core/data/response/ProgressRepo.kt` | Available progress query methods |
| `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/home/HomeRepository.kt` | How items + progress are combined for picker screens |
| `media/src/main/java/dev/halim/shelfdroid/media/service/PlaybackService.kt` | How to bind `MediaController` in `ActionCallback` |
| `gradle/libs.versions.toml` | Fix version ref bug before adding dependency |
| `app/build.gradle.kts` | Add `implementation(libs.androidx.glance)` |
| `app/src/main/AndroidManifest.xml` | Add receivers + activities |

---

## Reusable utilities

| Utility | Location | Usage |
|---------|----------|-------|
| `ProgressRepo.bookById()` | `core-data` | Flow of book progress (currentTime, duration) |
| `ProgressRepo.episodeById()` | `core-data` | Flow of episode progress |
| `LibraryItemRepo.byId()` | `core-data` | Fetch any item by ID |
| `LibraryItemRepo.podcasts()` | `core-data` | List all podcasts for picker |
| `PodcastMapper` episode sort | `core-data` | Episodes already sorted `publishedAt DESC` — use `[0]` |
| Coil image loader | `ShelfDroid.kt` | `context.imageLoader.execute(request)` → `BitmapDrawable` → `ImageProvider` |
| `AdvancedControl.sleepTimerLeft` | `PlayerUiState` | `Duration` — ready to format for sleep timer button label |

---

## Verification checklist

1. `./gradlew :app:assembleDebug` — builds without errors
2. **Widget 1:** Start playing a book → add Playback widget → cover/title/author appear; tap play/pause → state toggles; tap rewind/forward → playback jumps; sleep timer button shows countdown when active
3. **Widget 2:** Add Book Remaining widget → book picker appears → select a book → widget shows correct remaining time matching the in-app progress bar
4. **Widget 3:** Add Latest Episode widget → podcast picker appears → select a podcast → widget shows newest episode title and correct publish date; tap Play → app opens at that episode
5. **Staleness test:** Advance playback 10 min, background the app, wait 15 s → Playback widget position updates; wait 30 min → Book Remaining widget updates
