# Android Auto Support

## Overview

ShelfDroid already has most of the playback infrastructure required for Android Auto:

- Media3 `1.11.0`
- an exported `MediaLibraryService`
- a `MediaLibrarySession`
- ExoPlayer-based foreground playback
- audiobook and podcast episode metadata
- artwork and media notification support
- play, pause, seek-back, seek-forward, and custom session commands

The missing work is primarily Android Auto discovery, a browsable media hierarchy, and resolving
car-originated media IDs into ShelfDroid's existing playback flow. The standard Android Auto media
experience does not require the Android for Cars App Library or a separate car UI.

This plan targets Android Auto, where ShelfDroid continues to run on the connected phone. Android
Automotive OS packaging and distribution are separate follow-up work.

---

## Current State

### Playback service

`media/src/main/java/dev/halim/shelfdroid/media/service/PlaybackService.kt` already extends
`MediaLibraryService` and creates a `MediaLibrarySession` around the shared ExoPlayer instance.

`media/src/main/AndroidManifest.xml` already:

- exports `PlaybackService`
- declares `foregroundServiceType="mediaPlayback"`
- registers the `androidx.media3.session.MediaSessionService` action
- registers the compatibility `android.media.browse.MediaBrowserService` action

The final merged manifest must continue to contain both `android.permission.FOREGROUND_SERVICE` and
`android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`.

### Session callback

`media/src/main/java/dev/halim/shelfdroid/media/di/PlayerModule.kt` currently provides an anonymous
`MediaLibrarySession.Callback`. It accepts controllers and exposes playback custom commands, but it
does not implement any library callbacks:

- `onGetLibraryRoot`
- `onGetChildren`
- `onGetItem`
- `onSearch`
- `onGetSearchResult`
- `onAddMediaItems` or `onSetMediaItems`

Android Auto can therefore connect to the playback session, but ShelfDroid does not yet expose a
browsable Catalog or a way to start new content from a browser request.

### Reusable Catalog and playback data

The existing data layer already provides most of the required inputs:

- `LibraryRepository` lists locally cached Libraries.
- `LibraryItemRepository` exposes the cached Catalog, Books, Podcasts, and cover URLs.
- `PodcastEpisodeRepository` provides Episodes by Podcast.
- `ProgressRepository` provides unfinished and recent Progress.
- `DownloadRepo` and the durable download catalogs can resolve offline media.
- `PlayerRepository.playBook()` and `playPodcast()` already assemble playback state, restore
  Progress, select downloaded or remote audio, and start an Audiobookshelf listening session.
- `MediaItemMapper` already produces playable Media3 items with title, artist, artwork, media type,
  URI, and clipping configuration.

The Android Auto implementation should reuse these paths instead of independently constructing
stream URLs or duplicating listening-session logic.

---

## Phase 1 — Android Auto Discovery

Add the Android Auto capability declaration to the application manifest:

```xml
<meta-data
    android:name="com.google.android.gms.car.application"
    android:resource="@xml/automotive_app_desc" />
```

Create `app/src/main/res/xml/automotive_app_desc.xml`:

```xml
<automotiveApp>
    <uses name="media" />
</automotiveApp>
```

Verify the merged manifest rather than relying only on individual library manifests. In particular,
confirm the foreground-service permissions and both playback-service intent actions are present.

**Files:**

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/automotive_app_desc.xml` (new)
- `media/src/main/AndroidManifest.xml` if permission ownership needs clarification

---

## Phase 2 — Car Media IDs and Browse Models

Introduce a typed media ID model specifically for the browse hierarchy. The current
`MediaIdWrapper`, which represents playable content as `itemId` or `itemId|secondaryId`, cannot
unambiguously represent root tabs, Libraries, Books, Podcasts, and Episodes.

Example IDs:

```text
car:root
car:continue
car:books
car:podcasts
car:downloads
car:library:<libraryId>
car:book:<bookId>
car:podcast:<podcastId>
car:episode:<podcastId>:<episodeId>
```

Parsing must be total and defensive: malformed or stale IDs return a library error rather than
throwing. Keep compatibility with existing playable IDs where external controllers might already
hold one.

Create a mapper that converts ShelfDroid Catalog records into browser-facing `MediaItem` values.
Each item must provide the applicable fields:

- stable `mediaId`
- `isBrowsable` or `isPlayable`
- `MediaMetadata.mediaType`
- title and subtitle or artist
- artwork URI with a safe fallback
- completion state or percentage where useful

Browsable items must not expose playback URIs. Playable URIs should only be resolved when playback
is requested.

**Recommended files:**

- `media/src/main/java/dev/halim/shelfdroid/media/library/CarMediaId.kt` (new)
- `media/src/main/java/dev/halim/shelfdroid/media/library/CarMediaItemMapper.kt` (new)
- focused unit tests under `media/src/test/`

---

## Phase 3 — Browsable Catalog

Replace the anonymous library callback with a dedicated, injectable, testable implementation. Keep
the existing custom-command handling, but add the Media3 library contract.

### Root

`onGetLibraryRoot` must return quickly, including when ShelfDroid is signed out. Do not perform
network access, session recovery, or other slow authentication work before returning the root.

Design for at most four browsable root children, as requested by Android Auto root hints. A useful
initial hierarchy is:

```text
ShelfDroid
├── Continue listening
├── Books
│   └── Library → Books
├── Podcasts
│   └── Library → Podcast → Episodes
└── Downloads
```

If a root hint permits fewer than four children, prioritize Continue listening, Books, Podcasts,
then Downloads. Root children should have short labels and monochrome tab icons.

### Children and items

Implement:

- `onGetChildren` from the locally cached Catalog
- `onGetItem` for direct lookup and controller restoration
- library-change notifications when Catalog synchronization materially changes a visible node

Do not rely on `page` and `pageSize`; Android Auto and Android Automotive OS media browsers do not
support pagination. Bound large child lists deliberately and provide meaningful, shallow routes so
drivers do not have to traverse the full Catalog.

The first implementation should remain useful without a network connection. Refreshing from the
Audiobookshelf server may happen asynchronously, but browsing must not wait indefinitely for it.

**Recommended files:**

- `media/src/main/java/dev/halim/shelfdroid/media/library/ShelfMediaLibraryCallback.kt` (new)
- `media/src/main/java/dev/halim/shelfdroid/media/library/CarCatalogRepository.kt` (new, if a small
  read facade is needed across the existing repositories)
- `media/src/main/java/dev/halim/shelfdroid/media/di/PlayerModule.kt`
- callback and hierarchy tests under `media/src/test/`

---

## Phase 4 — Resolve Car Playback Requests

Selecting an Android Auto browse result generally sends ShelfDroid a media ID without a playable
URI. Implement `onAddMediaItems` or `onSetMediaItems` to:

1. Parse the typed media ID.
2. Validate that the selected Book or Episode still exists and is accessible to the current User.
3. Call the existing `PlayerRepository.playBook()` or `playPodcast()` path.
4. Reuse `MediaItemMapper` to produce the complete playable item or Track list.
5. Return a failed session result with an actionable error when content cannot be resolved.

Extract a small application-scoped playback coordinator if necessary so both phone UI requests and
car requests enter the same pipeline. Do not make the callback navigate through `MainActivity` to
start playback; Android Auto must work when no Activity exists or can be shown.

Preserve existing behavior for:

- saved Progress
- Book Chapter and Track mapping
- Episode playback
- offline Downloads
- playback speed preferences
- listening-session synchronization

**Recommended files:**

- `media/src/main/java/dev/halim/shelfdroid/media/library/CarPlaybackCoordinator.kt` (new)
- `media/src/main/java/dev/halim/shelfdroid/media/library/ShelfMediaLibraryCallback.kt`
- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/player/PlayerRepository.kt` only if
  a narrower reusable interface is required
- `media/src/main/java/dev/halim/shelfdroid/media/mediaitem/MediaItemMapper.kt`

---

## Phase 5 — Service Lifecycle and Error States

Android Auto can instantiate `PlaybackService` before ShelfDroid's Activity has ever opened. Test
and support all of these states explicitly:

- signed in with a warm Catalog cache
- signed in with an empty or stale cache
- signed out
- expired credentials or failed Session recovery
- Audiobookshelf server unavailable
- local-network server without the required permission
- downloaded content available while offline

When User action is required, expose a car-appropriate error such as "Sign in on your phone when it
is safe." Never launch the phone UI automatically while driving.

Revisit two service lifecycle decisions in `PlaybackService`:

- `onTaskRemoved()` currently always calls `pauseAllPlayersAndStopSelf()`. Swiping ShelfDroid from
  phone recents would stop active Android Auto playback. Prefer Media3's default ongoing-playback
  behavior or stop only when playback is not ongoing.
- `onDestroy()` currently calls `Process.killProcess()`. Remove this and explicitly release resources
  owned by the service. External controllers and normal Android service lifecycle events must not
  terminate the complete ShelfDroid process.

Also use `MediaSession.ControllerInfo` helpers where behavior differs by controller. In particular,
grant any intended custom commands to `session.isAutoCompanionController(controller)` and configure
the media-notification controller separately when necessary.

**Files:**

- `media/src/main/java/dev/halim/shelfdroid/media/service/PlaybackService.kt`
- `media/src/main/java/dev/halim/shelfdroid/media/library/ShelfMediaLibraryCallback.kt`
- authentication/session state adapters as required

---

## Phase 6 — Voice Search and Playback Resumption

### Search

Implement Media3 library search so Android Auto and Google Assistant can find Books, Podcasts, and
Episodes by title and, where useful, author. Handle an empty query as a general playback request by
resuming the most recent unfinished content.

Search work must be asynchronous and bounded. Prefer indexed local Catalog queries rather than
scanning large in-memory collections or waiting for network search.

This overlaps with Part 1 of `docs/plan/google-assistant-integration.md`. The shared
`ShelfMediaLibraryCallback` and playback coordinator from this plan should own the common Media3
browse, search, and playback-resolution behavior instead of adding more methods to the anonymous
callback in `PlayerModule`.

### Resumption

Add Media3 playback resumption after browse and playback are stable:

1. Declare `androidx.media3.session.MediaButtonReceiver`.
2. Implement `MediaSession.Callback.onPlaybackResumption()`.
3. Restore the most recent playable item or queue and its saved position.
4. Restore relevant playback speed and other persisted playback preferences.
5. Return locally available metadata and artwork quickly, including during boot when network access
   may be unavailable.

Do not declare `MediaButtonReceiver` until `onPlaybackResumption()` is implemented; Media3 requires
both parts together.

---

## Phase 7 — Verification

### Automated tests

Add tests for:

- every typed media ID round trip
- malformed and stale IDs
- root child limits and ordering
- browsable versus playable metadata
- Book, Podcast, and Episode hierarchy mapping
- signed-out and offline results
- playback resolution for Books and Episodes
- local Download preference over remote playback
- search ranking and empty-query behavior
- custom-command availability for Android Auto controllers
- playback resumption state

### Integration tests

Use the Media Controller Test app to verify the Media3 contract independently from Android Auto.
Use the Android Auto Desktop Head Unit for the complete experience.

Test at least:

1. `PlaybackService` starts before any Activity.
2. Force-stop ShelfDroid, then open it from Android Auto.
3. Clear app data, then open it from Android Auto.
4. Browse and play while signed in.
5. Browse while signed out and receive an actionable error.
6. Browse and play Downloads with the server unreachable.
7. Play, pause, seek back, seek forward, and use steering-wheel media buttons.
8. Switch audio focus between Android Auto and another source.
9. Run voice searches for a Book, Podcast, and Episode.
10. Exercise day/night mode, touch, rotary input, and driving restrictions.
11. Verify artwork and text on small, wide, and high-resolution DHU configurations.
12. Swipe ShelfDroid from phone recents while Android Auto playback is active.

Run the existing relevant Gradle test suites and add the DHU result to the manual release checklist.

---

## Phase 8 — Distribution

Before release:

1. Validate the Media category against the Android car app quality guidelines.
2. Upload an Android Auto-capable app bundle to a testing track.
3. In Play Console, open **Advanced settings → Form factors** and add Android Auto.
4. Provide review credentials under App Access because ShelfDroid requires an Audiobookshelf server
   and User sign-in.
5. Test through Internal App Sharing or an internal testing track before production review.

Android Auto is delivered as part of the existing mobile app. A dedicated Android Automotive OS
artifact and track are not required for this plan.

---

## Recommended Commit Sequence

Use Conventional Commit titles in Commitizen format.

1. `feat(auto): declare Android Auto media capability`
2. `feat(auto): add typed car media identifiers`
3. `feat(auto): expose the cached catalog to media browsers`
4. `feat(auto): resolve browser selections into playback`
5. `fix(media): support playback without an active phone task`
6. `feat(auto): add car search and playback resumption`
7. `test(auto): cover media browsing and playback flows`

Each commit should build and keep existing phone playback tests passing.

---

## MVP Exit Criteria

Android Auto support is minimally complete when:

- Android Auto discovers ShelfDroid as a media app.
- The four-root hierarchy renders from the cached Catalog.
- A User can browse and play a Book or Episode without opening the phone Activity.
- play, pause, seek back, and seek forward work from the car.
- signed-out, offline, and unavailable-server states do not hang or crash the service.
- active playback survives dismissal of the phone task.
- Media Controller Test and DHU verification pass.
- the implementation meets the applicable Media car quality requirements.

Voice search and playback resumption are strongly recommended for the first public release, but they
can follow the basic browse-and-play MVP if necessary.

---

## Official References

- [Add support for Android Auto to a media app](https://developer.android.com/training/cars/media/auto)
- [Serve content with a MediaLibraryService](https://developer.android.com/media/media3/session/serve-content)
- [Create a content hierarchy for car media apps](https://developer.android.com/training/cars/media/create-media-browser/content-hierarchy)
- [Media3 background playback and resumption](https://developer.android.com/media/media3/session/background-playback)
- [Support voice actions](https://developer.android.com/training/cars/media/voice-actions)
- [Test using the Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu)
- [Car app quality](https://developer.android.com/docs/quality-guidelines/car-app-quality)
- [Distribute to cars](https://developer.android.com/training/cars/distribute)
