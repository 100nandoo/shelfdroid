# Glance Playback Widget — Implementation Plan

## Context

This file only covers **Widget 1: Playback Control**.

Widget 2 and Widget 3 have been moved to `docs/plan/glance-compose-configurable-widgets.md` because they introduce a different problem shape: per-widget configuration, persisted widget state, and picker activities.

Current Android docs checked on 2026-06-10 list Glance **1.1.1** as stable, **1.2.0-rc01** as release candidate, and **1.3.0-alpha01** as alpha. The repo currently declares `androidxGlance = "1.2.1"` in `libs.versions.toml`, but its version-catalog wiring is incorrect and there is no dedicated widget module yet. No widget code exists yet.

---

## Pre-work: Create `:widget` and add the correct Glance artifacts

**File:** `settings.gradle.kts`

```kotlin
include(":widget")
```

**File:** `gradle/libs.versions.toml`

```toml
# add module alias
widget = ":widget"

# CURRENTLY BROKEN
androidx-datastore = { module = "androidx.datastore:datastore-preferences", version.ref = "androidxGlance" }
androidx-glance = { module = "androidx.glance:glance-material3", version.ref = "androidxDatastore" }

# FIX
androidx-datastore = { module = "androidx.datastore:datastore-preferences", version.ref = "androidxDatastore" }
androidx-glance-appwidget = { module = "androidx.glance:glance-appwidget", version.ref = "androidxGlance" }
androidx-glance-material3 = { module = "androidx.glance:glance-material3", version.ref = "androidxGlance" }
```

**File:** `widget/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "${libs.versions.namespace.get()}.widget"
    compileSdk = libs.versions.targetSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
        buildConfig = false
        shaders = false
    }
}

dependencies {
    implementation(project(libs.versions.core.get()))
    implementation(project(libs.versions.coreData.get()))
    implementation(project(libs.versions.helper.get()))
    implementation(project(libs.versions.media.get()))

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

**File:** `app/build.gradle.kts`

```kotlin
dependencies {
    implementation(project(libs.versions.widget.get()))
}
```

Notes:

- Pick an explicit Glance track before implementation. The Android docs currently show `1.1.1` stable, `1.2.0-rc01` RC, and `1.3.0-alpha01` alpha.
- Keep `glance-appwidget` and `glance-material3` separate. `glance-material3` alone is not enough for widgets.
- If periodic background refresh remains in scope, add WorkManager setup separately. No existing `CoroutineWorker`, `WorkManager`, or `@HiltWorker` wiring was found in the repo.
- Use an Android library module for widgets. Its manifest and resources merge into the final app package at build time.

---

## Module strategy

Start with a dedicated **`:widget`** Android library module from phase 1.

Why:

- widget receivers, provider XML, and widget-only resources stay out of `:app`
- the repo already uses feature and infrastructure modules heavily, so adding one more module is consistent with the current structure
- Widget 2 and 3 will need more files, state handling, and configuration screens, so the separation will pay off early
- the final app still stays simple because the `:widget` manifest and resources merge into `:app` automatically

Package root: `dev.halim.shelfdroid.widget.playback`

Recommendation:

- keep application/bootstrap concerns in `:app`
- keep widget receivers, provider XML, widget actions, widget-specific utilities, and future configuration activities in `:widget`
- only pull `core-ui` into `:widget` if the future config activities truly need shared theme or UI components

---

## Widget 1 — Playback Control

**Purpose:** Show now-playing info and transport controls (play/pause, +/-30 s, sleep timer) on the home screen.

### Data source

| Field | Source |
|-------|--------|
| `title`, `author`, `cover` | `PlayerUiState` via `PlayerStore.uiState` |
| Play / Pause state | `PlayerUiState.playPause` (`PlayPauseControlState`) |
| Sleep timer remaining | `PlayerUiState.advancedControl.sleepTimerLeft` (`Duration`) |
| Transport commands | Widget-safe Glance actions (`actionRunCallback`, `actionStartService`, or `actionSendBroadcast`) |

### Files to create

| Path | Purpose |
|------|---------|
| `widget/src/main/java/dev/halim/shelfdroid/widget/playback/PlaybackWidget.kt` | `GlanceAppWidget` UI |
| `widget/src/main/java/dev/halim/shelfdroid/widget/playback/PlaybackWidgetReceiver.kt` | `GlanceAppWidgetReceiver` |
| `widget/src/main/java/dev/halim/shelfdroid/widget/playback/PlaybackWidgetWorker.kt` | Optional worker, only if periodic background refresh is still required |
| `widget/src/main/java/dev/halim/shelfdroid/widget/playback/PlaybackActions.kt` | `ActionCallback` classes |
| `widget/src/main/res/xml/playback_widget_info.xml` | `appwidget-provider` XML |

### UI layout

```
Row {
    Image(ImageProvider(coverBitmap)) // Load cover off-main-thread, then hand Glance an ImageProvider
    Column {
        Text(title)
        Text(author)
        Row {
            Button(rewind 30s)
            Button(play / pause)      // icon swaps on playPause.showPlayIcon
            Button(fast-forward 30s)
            Button(sleep timer)       // shows "12 min" when active, clock icon otherwise
        }
    }
}
```

### Action callbacks

| Callback | Command |
|----------|---------|
| `PlayPauseAction` | Create a widget-scoped `MediaController` from `SessionToken`, then `play()` / `pause()` |
| `RewindAction` | Create a widget-scoped `MediaController`, then `seekBack()` |
| `FastForwardAction` | Create a widget-scoped `MediaController`, then `seekForward()` |
| `SleepTimerAction` | Explicit service or broadcast action handled by the app |

Notes:

- Do not reuse `MediaControllerManager` from the activity layer. It is `ActivityRetainedScoped` and not suitable for widget callbacks.
- For Widget 1, no configuration activity is needed. The widget has a natural default state: "current playback".
- If `ActionCallback` work becomes long-running, offload it to a worker and then call a Glance update.

### Update strategy

- Update immediately while the app or `PlaybackService` is awake by calling `PlaybackWidget().update(context, glanceId)` for a single instance or `PlaybackWidget().updateAll(context)` for all instances.
- If background freshness is still required when the app is not awake, use WorkManager at **15 min or slower**, not 15 seconds.
- Avoid minute-level background polling for playback state. The current Android guidance explicitly warns against overly frequent background updates.

---

## AndroidManifest additions (`widget/src/main/AndroidManifest.xml`)

```xml
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
```

## Provider XML requirements (`widget/src/main/res/xml/playback_widget_info.xml`)

- Set `android:initialLayout="@layout/glance_default_loading_layout"`
- Define realistic `minWidth`, `minHeight`, `targetCellWidth`, `targetCellHeight`, and resize bounds
- Add `android:description`
- Add at least a `previewImage` fallback; add `previewLayout` if practical
- If the widget is meaningfully resizable, explicitly choose `SizeMode.Responsive` or `SizeMode.Exact` in the `GlanceAppWidget` implementation instead of relying on defaults

## Optional: Generated previews on Android 15+

- Glance supports generated widget previews via `GlanceAppWidget.providePreview` and `GlanceAppWidgetManager.setWidgetPreviews`
- This is not required for the first implementation, but it is worth planning after the widget is stable

---

## Critical files to read before implementing

| File | Why |
|------|-----|
| `core/src/main/java/dev/halim/shelfdroid/core/PlayerUiState.kt` | Full playback state shape |
| `media/src/main/java/dev/halim/shelfdroid/media/service/PlayerStore.kt` | Source of reactive playback UI state |
| `media/src/main/java/dev/halim/shelfdroid/media/service/PlaybackService.kt` | Playback state source and service lifecycle |
| `media/src/main/java/dev/halim/shelfdroid/media/di/ActivityModule.kt` | Current `MediaController` wiring that must not be reused as-is in widgets |
| `settings.gradle.kts` | Register the new `:widget` module |
| `gradle/libs.versions.toml` | Fix swapped DataStore / Glance aliases and add appwidget artifact |
| `widget/build.gradle.kts` | Define the widget library module and its dependencies |
| `app/build.gradle.kts` | Add the `:widget` dependency |
| `widget/src/main/AndroidManifest.xml` | Add widget receiver |

---

## Reusable utilities

| Utility | Location | Usage |
|---------|----------|-------|
| `PlayerStore.uiState` | `media` | Current title, author, cover, playback state, sleep timer state |
| `PlayerUiState.playPause` | `core` | Decide play/pause icon and enabled state |
| `AdvancedControl.sleepTimerLeft` | `core` | Format sleep timer state |
| App-scoped Coil image loader | `ShelfDroid.kt` | Load cover bitmaps, then convert to `ImageProvider` for Glance |
| `PlaybackService` session component | `media` | Session target for widget-scoped `MediaController` creation |

---

## Verification checklist

1. `./gradlew :app:assembleDebug` builds without errors
2. Start playing a book -> add Playback widget -> cover, title, and author appear
3. Tap play/pause -> playback state toggles correctly
4. Tap rewind/forward -> playback jumps correctly
5. Sleep timer state reflects active / inactive state correctly
6. While playback is active and the app / service is awake, widget updates happen through `update(context, glanceId)` or `updateAll(context)` rather than a polling loop
7. Widget picker shows description, preview fallback, and loading state correctly
