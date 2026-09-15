# Media3 1.10 ShelfDroid Findings

## Overview

Media3 1.10.0 contains several changes that directly affect ShelfDroid's audiobook and podcast
playback stack. The most valuable changes are safer media artwork transport, improved gapless and
offloaded audio playback, more reliable progressive playlists, and media session and notification
fixes.

ShelfDroid already uses Media3 1.11.1, so this document records inherited behavior and one local
configuration issue discovered while assessing the 1.10.0 release.

---

## High-Relevance Changes

### Large artwork transport

`media/src/main/java/dev/halim/shelfdroid/media/mediaitem/MediaItemMapper.kt` reads the complete
cached cover image into a `ByteArray` and passes it to `MediaMetadata.Builder.setArtworkData()`.

Media3 1.10.0 fixes crashes caused by large `MediaMetadata.artworkData` arrays during media session
and Binder transport. This directly improves the safety of ShelfDroid's current metadata mapping.

The Media3 fix reduces crash risk but does not remove the cost of loading the complete image into
memory. Artwork size normalization remains a possible follow-up if memory use or transaction size
becomes a problem.

### Gapless compressed-offload playback

`media/src/main/java/dev/halim/shelfdroid/media/di/PlayerModule.kt` creates
`AudioOffloadPreferences` that request:

- audio offload
- gapless support
- playback-speed support

Media3 1.10.0 fixes playback becoming stuck between items in a gapless playlist when compressed
offload is active. This is particularly relevant to multi-file audiobooks.

However, ShelfDroid currently builds updated track-selection parameters without assigning them back
to the player:

```kotlin
player.trackSelectionParameters
  .buildUpon()
  .setAudioOffloadPreferences(audioOffloadPreferences)
  .build()
```

The built value is discarded, so the intended offload preferences may not be active. Assign the
result explicitly:

```kotlin
player.trackSelectionParameters =
  player.trackSelectionParameters
    .buildUpon()
    .setAudioOffloadPreferences(audioOffloadPreferences)
    .build()
```

This correction should be covered by a focused test or by inspecting the configured
`trackSelectionParameters` after player construction.

### Progressive playlist reliability

ShelfDroid supplies cached and remote data through `DefaultMediaSourceFactory`. Media3 1.10.0 fixes
stale `ProgressiveMediaSource` timeline information that could remove queued periods unexpectedly.
This benefits chapter-based playback where an audiobook is represented as multiple media items.

### Media session and notification reliability

`media/src/main/java/dev/halim/shelfdroid/media/service/PlaybackService.kt` uses
`MediaLibraryService` and `MediaLibrarySession`, while
`media/src/main/java/dev/halim/shelfdroid/media/notification/CustomMediaNotificationProvider.kt`
customizes notification actions.

Relevant Media3 1.10.0 changes include:

- foreground-service startup handling for stale intents
- safe Android 10 artwork scaling
- a workaround for a System UI crash caused by particular artwork sizes
- correct transition callbacks between equal `MediaItem` values
- synchronized media-library subscription access
- `MediaSessionService` and `MediaLibraryService` becoming `LifecycleService` subclasses

ShelfDroid's custom notification provider extends `DefaultMediaNotificationProvider`, so the new
method added to the unstable `MediaNotification.Provider` interface does not require a direct local
implementation.

---

## Optional Opportunities

### Lifecycle-aware service work

Because `MediaLibraryService` is now a `LifecycleService`, playback-service observers and coroutines
can be attached to its lifecycle. ShelfDroid currently owns a separate `serviceScope`. Migrating it
is optional and should only be done if it simplifies cancellation and service teardown.

### Stable mute and track preferences

Media3 1.10.0 stabilizes `Player.mute()`, `Player.unmute()`, PCM encoding APIs, and several track
language and label preferences. These APIs do not provide an immediate ShelfDroid improvement but
may be useful for future mute controls or multi-track audiobook selection.

### Compose player controls

Media3 1.10.0 adds Compose playback-speed controls, a progress slider, and a Material 3 `Player`.
ShelfDroid already has custom player UI, state, seeking, playback-speed behavior, and shared-element
transitions. Replacing these controls would provide little benefit and would reduce product-specific
behavior, so no migration is recommended.

---

## Low-Relevance Changes

The following Media3 1.10.0 areas are not currently significant because ShelfDroid constructs an
audio-only renderer and does not use the affected features:

- Dolby Vision Profile 10 and VVC/H.266 video
- video renderer scheduling and video frame extraction
- Transformer and CompositionPlayer
- Lottie video effects
- IMA advertising
- Cast playback
- IAMF spatial audio
- most DASH and RTSP changes

HLS fixes could matter for a server-provided HLS audio URL, but ShelfDroid's normal audiobook and
podcast paths primarily benefit from the progressive-media fixes.

---

## Recommended Action

Fix the discarded `TrackSelectionParameters` result in `PlayerModule.kt`, then verify that the
constructed player contains the requested `AudioOffloadPreferences`. No other Media3 1.10-specific
migration is currently required.

Official release notes:
https://developer.android.com/jetpack/androidx/releases/media3#1.10.0
