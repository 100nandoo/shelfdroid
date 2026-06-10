# Glance Configurable Widgets — Implementation Plan

## Context

This file covers the two widgets that require **per-widget configuration**:

- Widget 2: Book Remaining Time
- Widget 3: Latest Podcast Episode

These were split out from the playback widget plan because they need persisted widget state and configuration activities, while Widget 1 does not.

This plan assumes the shared Glance dependency, version-catalog fixes, and dedicated `:widget` module bootstrap from `docs/plan/glance-compose-widgets.md`.

When Widget 2 or 3 is started, extend `widget/build.gradle.kts` with the dependencies needed for configuration screens, for example:

- `implementation(libs.androidx.activity.compose)`
- Compose BOM plus `ui`, `ui-tooling-preview`, and `material3`
- optionally `implementation(project(libs.versions.coreUi.get()))` if the config screens should reuse the existing app theme or shared UI components

---

## Why configuration activities are needed

These widgets are not global widgets like "current playback". Each widget instance must know which specific book or podcast it represents.

That means each widget instance needs:

- a selected item ID
- persisted per-widget state
- a first-run selection flow

On Android, the standard way to do that is an `android:configure` activity launched when the widget is added. That is why this plan uses `BookRemainingConfigActivity` and `LatestEpisodeConfigActivity`.

If you want to avoid configuration activities entirely, the widgets need a safe default selection strategy, for example "most recently played book" or "most recently updated podcast". That would be a product decision, not just an implementation detail.

---

## Widget 2 — Book Remaining Time

**Purpose:** Show a specific book's cover, title, and "X h Y min remaining" on the home screen.

### Data source

| Field | Source |
|-------|--------|
| `title`, `cover` | `LibraryItemRepo.byId(libraryItemId)` |
| `currentTime`, `duration` | `ProgressRepo.bookById(libraryItemId)` -> `ProgressEntity` |
| Remaining time | `duration - currentTime` (seconds) -> formatted as `"Xh Ymin"` |
| Chosen book | Stored per-widget in Glance preferences state via `updateAppWidgetState(...)` |

### State key

```kotlin
val KEY_LIBRARY_ITEM_ID = stringPreferencesKey("library_item_id")
```

### Files to create

| Path | Purpose |
|------|---------|
| `widget/src/main/java/dev/halim/shelfdroid/widget/book/BookRemainingWidget.kt` | `GlanceAppWidget` |
| `widget/src/main/java/dev/halim/shelfdroid/widget/book/BookRemainingWidgetReceiver.kt` | `GlanceAppWidgetReceiver` |
| `widget/src/main/java/dev/halim/shelfdroid/widget/book/BookRemainingWorker.kt` | Optional worker, only if stale background refresh is required |
| `widget/src/main/java/dev/halim/shelfdroid/widget/book/BookRemainingConfigActivity.kt` | Config activity for picking a book |
| `widget/src/main/res/xml/book_remaining_widget_info.xml` | `appwidget-provider` XML with `android:configure` |

### UI layout

```
Column {
    Image(ImageProvider(coverBitmap))
    Text(title, maxLines = 2)
    Text("3 h 4 min remaining")
}
```

### Configuration flow

1. User adds widget -> system launches `BookRemainingConfigActivity`
2. Activity reads `AppWidgetManager.EXTRA_APPWIDGET_ID` from the launch intent
3. Activity immediately sets `RESULT_CANCELED` so backing out does not leave behind a broken widget
4. Activity shows list of books from `HomeRepository`
5. On selection, writes `libraryItemId` to widget preferences with `updateAppWidgetState(...)`
6. Activity resolves the platform widget ID to a `GlanceId` with `GlanceAppWidgetManager.getGlanceIdBy(appWidgetId)`
7. Activity calls `BookRemainingWidget().update(context, glanceId)`
8. Activity finishes with `RESULT_OK`

### Update strategy

- Always update immediately after configuration and after any in-app sync that changes progress
- If the widget still needs background refresh while the app is asleep, use `updatePeriodMillis` at **30 min or slower** or WorkManager at **15 min or slower**
- Do not assume a worker is required on day one. This repo currently has no WorkManager setup

---

## Widget 3 — Latest Podcast Episode

**Purpose:** Show the newest episode for a chosen podcast, with a button that opens the app at that episode.

### Data source

| Field | Source |
|-------|--------|
| Podcast `title`, `cover` | `LibraryItemRepo.byId(libraryItemId)` |
| Latest episode | Decode the stored `Podcast` entity and sort `episodes` by `publishedAt DESC` locally |
| Episode `title`, `publishedAt` | `PodcastEpisode.title`, `PodcastEpisode.publishedAt` (Unix ms -> formatted date) |
| Chosen podcast | Stored per-widget in Glance preferences state |

### Files to create

| Path | Purpose |
|------|---------|
| `widget/src/main/java/dev/halim/shelfdroid/widget/podcast/LatestEpisodeWidget.kt` | `GlanceAppWidget` |
| `widget/src/main/java/dev/halim/shelfdroid/widget/podcast/LatestEpisodeWidgetReceiver.kt` | `GlanceAppWidgetReceiver` |
| `widget/src/main/java/dev/halim/shelfdroid/widget/podcast/LatestEpisodeWorker.kt` | Optional worker, only if periodic background refresh is required |
| `widget/src/main/java/dev/halim/shelfdroid/widget/podcast/LatestEpisodeConfigActivity.kt` | Config activity for picking a podcast |
| `widget/src/main/res/xml/latest_episode_widget_info.xml` | `appwidget-provider` XML with `android:configure` |

### UI layout

```
Column {
    Row {
        Image(ImageProvider(podcastCoverBitmap))
        Text(podcastTitle)
    }
    Text(episodeTitle, maxLines = 2)
    Text("Apr 20, 2026")
    Button("Open") -> activity intent using `MediaIdWrapper(itemId, episodeId).toMediaId()`
}
```

### Update strategy

- Update on initial configuration and after any in-app library sync that changes podcast metadata
- If periodic background refresh is still required, use WorkManager at **15 min or slower** or `updatePeriodMillis` at **30 min or slower**
- Keep the first version simple: open the app at the episode. A true playback-start action would require new widget-specific playback-start plumbing

---

## AndroidManifest additions (`widget/src/main/AndroidManifest.xml`)

```xml
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

## Provider XML requirements (`widget/src/main/res/xml/*_widget_info.xml`)

- Set `android:initialLayout="@layout/glance_default_loading_layout"`
- Define realistic `minWidth`, `minHeight`, `targetCellWidth`, `targetCellHeight`, and resize bounds
- Add `android:description`
- Add at least a `previewImage` fallback; add `previewLayout` if practical
- For configurable widgets, set `android:configure` to the fully qualified activity name
- Only add `android:widgetFeatures="reconfigurable|configuration_optional"` if the widget has a safe default configuration that can skip the first-run picker
- If the widget is meaningfully resizable, explicitly choose `SizeMode.Responsive` or `SizeMode.Exact`

## Optional: Generated previews on Android 15+

- Glance supports generated widget previews via `GlanceAppWidget.providePreview` and `GlanceAppWidgetManager.setWidgetPreviews`
- This is not required for the first implementation, but it is worth planning after the widgets are stable

---

## Critical files to read before implementing

| File | Why |
|------|-----|
| `widget/build.gradle.kts` | Add Compose Activity and UI dependencies once config screens are introduced |
| `core-data/src/main/java/dev/halim/shelfdroid/core/data/response/ProgressRepo.kt` | Available progress query methods |
| `core-data/src/main/java/dev/halim/shelfdroid/core/data/response/LibraryItemRepo.kt` | Item lookup and podcast picker data |
| `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/home/HomeRepository.kt` | Book picker source |
| `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/MainActivity.kt` | Existing app-entry intent handling for widget deep links |
| `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/PendingMediaIdHandler.kt` | How `media_id` routes to book / podcast / episode screens |
| `media/src/main/java/dev/halim/shelfdroid/media/mediaitem/MediaIdWrapper.kt` | Build `media_id` values for navigation intents |

---

## Reusable utilities

| Utility | Location | Usage |
|---------|----------|-------|
| `ProgressRepo.bookById()` | `core-data` | Snapshot lookup of book progress |
| `ProgressRepo.flowBookById()` | `core-data` | Flow of book progress if reactive refresh is needed |
| `ProgressRepo.episodeById()` | `core-data` | Snapshot lookup of episode progress |
| `ProgressRepo.flowEpisodeById()` | `core-data` | Flow of episode progress if reactive refresh is needed |
| `LibraryItemRepo.byId()` | `core-data` | Fetch any item by ID |
| `LibraryItemRepo.podcastInfoList(libraryId)` | `core-data` | Podcast picker source when grouped by library |
| `MediaIdWrapper.toMediaId()` | `media` | Build `media_id` for book / episode navigation intents |
| App-scoped Coil image loader | `ShelfDroid.kt` | Load cover bitmaps, then convert to `ImageProvider` for Glance |
