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
    implementation(libs.androidx.media3.session)
    implementation(libs.coil)
    implementation(libs.coil.okhttp)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.guava)
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
- Move the shared Coil `ImageLoader` Hilt binding out of `:core-ui` into a non-UI shared module before widget work begins. `:helper` is the pragmatic first home in phase 1.

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
- do not pull `:core-ui` into `:widget` just to follow app theme in phase 1; read the same theme preferences and mirror them with widget-local Glance theming

---

## Widget 1 — Playback Control

**Purpose:** Show **Current playback** info and transport controls (play/pause, +/-10 s, sleep timer) on the home screen.

### Data source

| Field | Source |
|-------|--------|
| Primary text | `PlayerUiState.title` via `PlayerStore.uiState` |
| Secondary text | `PlayerUiState.author` via `PlayerStore.uiState` |
| `cover` | `PlayerUiState.cover` via `PlayerStore.uiState` |
| Play / Pause state | `PlayerUiState.playPause` (`PlayPauseControlState`) |
| Sleep timer remaining | `PlayerUiState.advancedControl.sleepTimerLeft` (`Duration`) |
| Transport commands | Widget-safe Glance actions (`actionRunCallback`, `actionStartService`, or `actionSendBroadcast`) |

Notes:

- This widget represents **Current playback**, not a server **Open session**.
- Text hierarchy should mirror the current big player semantics:
  - for a **Book**, the primary text is the current **Chapter** title when present, otherwise the **Book** title
  - for a **Podcast**, the primary text is the current **Episode** title
  - the secondary text remains the author line from `PlayerUiState.author`

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
        Text(title)   // primary line, matching the big player semantics
        Text(author)  // secondary line
        Row {
            Button(rewind 10s)
            Button(play / pause)      // icon swaps on playPause.showPlayIcon
            Button(fast-forward 10s)
            Button(sleep timer)       // shows "12 min" when active, clock icon otherwise
        }
    }
}
```

Empty state:

```
Column {
    Text("Nothing playing")
    Button("Open app")
}
```

### Action callbacks

| Callback | Command |
|----------|---------|
| `PlayPauseAction` | Create a widget-scoped `MediaController` from `SessionToken`, then `play()` / `pause()` |
| `RewindAction` | Create a widget-scoped `MediaController`, then `seekBack()` using the app's configured `10s` increment |
| `FastForwardAction` | Create a widget-scoped `MediaController`, then `seekForward()` using the app's configured `10s` increment |
| `SleepTimerAction` | Reuse the existing Media3 custom session command `CUSTOM_SLEEP_TIMER` |
| `OpenAppAction` | Launch the app when there is no **Current playback** or controller connection fails |

Notes:

- Do not reuse `MediaControllerManager` from the activity layer. It is `ActivityRetainedScoped` and not suitable for widget callbacks.
- For Widget 1, no configuration activity is needed. The widget has a natural default state: "current playback".
- If a widget action cannot connect a `MediaController`, fall back to opening the app instead of silently failing.
- If `ActionCallback` work becomes long-running, offload it to a worker and then call a Glance update.

### Update strategy

- Update immediately while the app or `PlaybackService` is awake by calling `PlaybackWidget().update(context, glanceId)` for a single instance or `PlaybackWidget().updateAll(context)` for all instances.
- Bridge the `:media` -> `:widget` boundary with a narrow interface such as `PlaybackWidgetSync` defined outside `:widget`, implemented in `:widget`, and injected into playback-layer code through Hilt. Do not create a reverse Gradle dependency from `:media` to `:widget`.
- If background freshness is still required when the app is not awake, use WorkManager at **15 min or slower**, not 15 seconds.
- Avoid minute-level background polling for playback state. The current Android guidance explicitly warns against overly frequent background updates.
- Follow app theme preferences by reading the same dark-mode and dynamic-theme settings used by the app and mapping them into a widget-local Glance theme.

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
| `media/src/main/java/dev/halim/shelfdroid/media/service/CustomCommand.kt` | Existing custom sleep-timer session command |
| `media/src/main/java/dev/halim/shelfdroid/media/di/ActivityModule.kt` | Current `MediaController` wiring that must not be reused as-is in widgets |
| `media/src/main/java/dev/halim/shelfdroid/media/di/PlayerModule.kt` | Current `10s` seek increments and custom session commands |
| `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/settings/SettingsRepository.kt` | Existing app theme preferences used by `MainActivity` |
| `settings.gradle.kts` | Register the new `:widget` module |
| `gradle/libs.versions.toml` | Fix swapped DataStore / Glance aliases and add appwidget artifact |
| `widget/build.gradle.kts` | Define the widget library module and its dependencies |
| `app/build.gradle.kts` | Add the `:widget` dependency |
| `widget/src/main/AndroidManifest.xml` | Add widget receiver |

---

## Reusable utilities

| Utility | Location | Usage |
|---------|----------|-------|
| `PlayerStore.uiState` | `media` | **Current playback** title, author, cover, playback state, sleep timer state |
| `PlayerUiState.playPause` | `core` | Decide play/pause icon and enabled state |
| `AdvancedControl.sleepTimerLeft` | `core` | Format sleep timer state |
| Shared Coil `ImageLoader` Hilt binding | shared non-UI module | Load cover bitmaps, then convert to `ImageProvider` for Glance |
| `PlaybackService` session component | `media` | Session target for widget-scoped `MediaController` creation |
| `CUSTOM_SLEEP_TIMER` | `media` | Reuse the existing sleep timer toggle command |

---

## Verification checklist

1. `./gradlew :app:assembleDebug` builds without errors
2. With active **Current playback**, add Playback widget -> cover, primary title, and secondary author line appear using the same content hierarchy as the big player
3. For a **Book**, verify the primary line shows the current **Chapter** title when present, otherwise the **Book** title
4. For a **Podcast**, verify the primary line shows the current **Episode** title
5. Tap play/pause -> playback state toggles correctly
6. Tap rewind/forward -> playback jumps by the app's existing `10s` increments
7. Sleep timer action toggles through the existing `CUSTOM_SLEEP_TIMER` behavior
8. With no **Current playback**, the widget shows the empty state and `Open app` action
9. While playback is active and the app / service is awake, widget updates happen through `update(context, glanceId)` or `updateAll(context)` rather than a polling loop
10. Widget theme follows the same dark-mode and dynamic-theme preferences as the app
11. Widget picker shows description, preview fallback, and loading state correctly
