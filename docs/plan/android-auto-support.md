# Android Auto Support

Lifecycle: proposed
Updated: 2026-11-25

## Overview

ShelfDroid already has most of the playback infrastructure required for Android Auto:

- Media3 `1.11.1`
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
- registers the `androidx.media3.session.MediaSessionService` action, but not the
  `androidx.media3.session.MediaLibraryService` action recommended for library discovery
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
- `onAddMediaItems` or `onSetMediaItems` for browse selections and voice playback requests

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

Add the Android Auto capability declaration to `<application>` in
`app/src/main/AndroidManifest.xml`:

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

All build variants, including public release builds, inherit this declaration. A build shipped after
Phase 1 can advertise ShelfDroid to Android Auto before the host can open its Catalog or start
playback from a browse selection.

### App branding

- **Launcher icon and app label**. Reuse the existing application-level
  `android:icon="@mipmap/ic_launcher"` and `android:label="@string/app_name"`. Android Auto
  uses these by default. Set the corresponding attributes on `PlaybackService` only if the car
  needs a different icon or label.
- **Attribution icon**. Reuse the existing monochrome notification vector, `ic_notification`,
  for media cards.
- **Accent color**. Set the framework `android:colorAccent` in `Theme.ShelfDroid.Car` to a dedicated
  color resource with the value `#FFC981`. This makes the car accent consistent across build
  variants, including debug, where the launcher background color is overridden. Point Android Auto
  to this style so the phone theme keeps its own accent.

Add both branding metadata entries under `<application>` in the same main manifest:

```xml
<meta-data
    android:name="androidx.car.app.TintableAttributionIcon"
    android:resource="@drawable/ic_notification" />
<meta-data
    android:name="com.google.android.gms.car.application.theme"
    android:resource="@style/Theme.ShelfDroid.Car" />
```

### Phase 1 acceptance

- Inspect the merged debug and release manifests for all three metadata entries and confirm the
  packaged media descriptor, icon, label, attribution vector, and accent style resolve.
- In both manifests, verify that `PlaybackService` is exported with `foregroundServiceType` set
  to `mediaPlayback`, advertises `androidx.media3.session.MediaLibraryService`, and retains the
  existing `androidx.media3.session.MediaSessionService` and
  `android.media.browse.MediaBrowserService` actions. Verify both
  `android.permission.FOREGROUND_SERVICE` and `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  through their existing manifest declarations.
- Install the debug build and connect a Media3 `MediaController` directly to `PlaybackService`.
- If the Desktop Head Unit shows ShelfDroid at this stage, check the launcher icon and label. The
  current callback rejects the library-root request, so verify that the host can open the Catalog
  in Phase 3. Verify media-card attribution and the accent on playback controls once those surfaces
  render.

**Files:**

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/automotive_app_desc.xml` (new)
- `app/src/main/res/values/themes.xml` and `app/src/main/res/values/colors.xml` for the car accent
- `media/src/main/AndroidManifest.xml` for the library service action

### Documentation

- [Add support for Android Auto to a media app](https://developer.android.com/training/cars/media/auto)
- [Configure the manifest for app icons, label, and accent color](https://developer.android.com/training/cars/media/configure-manifest)
- [Serve content with a MediaLibraryService](https://developer.android.com/media/media3/session/serve-content)

---

## Phase 2 — Car Media IDs and Browse Models

Introduce a typed media ID model specifically for the browse hierarchy. The current
`MediaIdWrapper`, which represents playable content as `itemId` or `itemId|secondaryId`, cannot
unambiguously represent root tabs, Libraries, Books, Podcasts, and Episodes.

Example IDs:

```text
car:root
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
- explicit values for both `isBrowsable` and `isPlayable` on every item returned in a
  `LibraryResult`, including the root
- `MediaMetadata.mediaType`
- title and subtitle or artist
- local artwork URI in `MediaMetadata.artworkUri`. Serve server covers through a `content://` URI
  and use an `android.resource://` URI for bundled icons and fallback artwork.
- completion state or percentage where useful

Browsable items must not expose playback URIs. Playable URIs should only be resolved when playback
is requested.

**Recommended files:**

- `media/src/main/java/dev/halim/shelfdroid/media/library/CarMediaId.kt` (new)
- `media/src/main/java/dev/halim/shelfdroid/media/library/CarMediaItemMapper.kt` (new)
- focused unit tests under `media/src/test/`

### Documentation

- [Media3 library callback contract](https://developer.android.com/reference/androidx/media3/session/MediaLibraryService.MediaLibrarySession.Callback)
- [Display media artwork](https://developer.android.com/training/cars/media/create-media-browser/media-artwork)

---

## Phase 3 — Browsable Catalog

Replace the anonymous library callback with a dedicated, injectable, testable implementation. Keep
the existing custom-command handling, but add the Media3 library contract.

### Root

`onGetLibraryRoot` must return quickly, including when ShelfDroid is signed out. Do not perform
network access, session recovery, or other slow authentication work before returning the root.

Expose up to three browsable root children. Read
`BROWSER_ROOT_HINTS_KEY_ROOT_CHILDREN_LIMIT` from `LibraryParams.extras`. Media3 converts the
legacy supported-flags hint to `EXTRA_KEY_ROOT_CHILDREN_BROWSABLE_ONLY` in those extras. The
initial hierarchy is:

```text
ShelfDroid
├── Books
│   └── [Book library] → Books
├── Podcasts
│   └── [Podcast library] → Podcast → Episodes
└── Downloads
    ├── Books → Downloaded Books
    └── Episodes → Downloaded Episodes
```

If a root hint permits fewer than three children, prioritize Books, Podcasts, then Downloads.
Use short labels and white tab icons that Android Auto can tint.

Resolve the library step independently for each tab using the locally cached Libraries:

- With multiple matching Libraries, show each as a browsable row using its Audiobookshelf name.
  Open the selected Library to show its Books or Podcasts.
- With one matching Library, open its Books or Podcasts directly from the tab.
- With no matching Libraries, return an empty list for the tab.

Books and Episodes are playable rows. Podcasts are browsable rows that open their Episodes.

### Downloads

Show Books and Episodes as separate browsable groups under Downloads whenever the User is signed
in, even if either group is empty. Use the existing local download checks against the current
Catalog. A Book appears when all its audio Tracks are available locally. An Episode appears when its
audio file is available locally. Apply the same list limits and grouping rules as the Catalog tabs.

### Children and items

Read `onGetChildren` and `onGetItem` from local data. Support direct lookup of the root, tabs,
Libraries, Podcasts, browse groups, Books, and Episodes so browser subscriptions and controller
restoration work. Validate a requested Episode against its Podcast and return a library error for
malformed, stale, or inaccessible IDs.

Android Auto and Android Automotive OS media browsers do not support pagination. Cap browse lists
at 50 rows before applying any page requested by another Media3 browser. Keep lists flat when they
fit. Group overflowing Books and Podcasts by title range and Episodes by publication year, newest
year first. Put undated Episodes in an “Unknown date” group. Subdivide any group that still exceeds
the limit, including groups with identical titles or dates, using a deterministic order and stable
tie-breakers. Apply
the same cap to Library rows and intermediate groups. Keep every cached entry reachable and add
depth only where the cap requires it. Give browse groups stable, typed IDs and short labels.

Notify browsers subscribed to an affected parent when its visible rows, order, or display metadata
change. This includes Catalog updates, changes in tab routing when Libraries are added or removed,
Download availability, and changes in access. Update the affected tabs, groups, and child lists so
a browser can reload the list it currently displays.

### Local access and synchronization

- Keep the root tabs available when signed out or during Forced re-login. Return empty tab contents
  and withhold direct lookup of protected content, including Downloads, until sign-in succeeds. A
  signed-in User can browse the cached Catalog and Downloads while offline. Phase 6 covers messages
  that prompt the User to act on the phone.
- Opening Books or Podcasts can start the existing `LibraryDataRepository` synchronization in the
  background without a phone Activity. Do so when the current User and server have no successful
  sync or the last successful sync was at least 15 minutes ago. Share the in-flight work across both
  tabs and other callers in the same session. After a failed attempt, wait at least one minute
  before another car-triggered attempt.
- Return available cached rows immediately. When the first tab request has no cached rows and a
  sync is in flight, give it up to three seconds, then return whatever is available. Notify the
  browser if rows arrive after that deadline. A successful sync with zero rows counts as fresh.
- Reconcile a successful empty Library response by removing its outdated cached rows. Preserve
  the last known rows for a Library whose request fails. Reuse the existing per-Library
  reconciliation described in ADR 0012.
- Make session isolation a prerequisite for car-triggered refresh. Scope freshness, retry state,
  and shared in-flight work to the current session. When Full logout or an Account switch clears
  the Catalog, a response from the prior session must never repopulate it. Requests for the new
  session must never join the prior session's in-flight job. This scopes ADR 0012's coalescing rule
  to callers in the same session. If OpenID returns a different User during Forced re-login, clear
  the previous session's Catalog and sync timestamp before making the new User's Catalog available.

### Phase 3 acceptance

- Verify that the root opens without an Activity or a network request, honors host tab limits, and
  retains the existing playback custom commands.
- Verify zero, one, and multiple Libraries independently for Books and Podcasts. Verify direct
  lookup of browsable and playable rows and errors for stale IDs and mismatched Podcast/Episode IDs.
- Exercise more than 50 Libraries, Books, Podcasts, and Episodes, including repeated titles and
  undated Episodes. Verify every entry is reachable and no response exceeds 50 rows. Use the Desktop
  Head Unit with long titles to check that Media3's response size limit does not hide rows.
- Verify both Downloads groups are present while signed in. Cover a partially downloaded Book, a
  completed multi-Track Book, and an Episode whose audio file is removed while its list is open.
- Exercise a warm Catalog offline, a cold request with a slow server, a successful empty response,
  and a failed refresh. Verify the three-second wait, 15-minute freshness window, one-minute retry
  cooldown, deduplication, and updates to subscribed parents.
- Switch accounts while a refresh is in flight and verify the old response cannot repopulate the
  Catalog. Verify signed-out and Forced re-login states retain the tabs while withholding content.

**Recommended files:**

- `media/src/main/java/dev/halim/shelfdroid/media/library/ShelfMediaLibraryCallback.kt` (new)
- `media/src/main/java/dev/halim/shelfdroid/media/library/CarCatalogRepository.kt` (new, if a small
  read facade is needed across the existing repositories)
- `media/src/main/java/dev/halim/shelfdroid/media/di/PlayerModule.kt`
- callback and hierarchy tests under `media/src/test/`

### Documentation

- [Create a content hierarchy for car media apps](https://developer.android.com/training/cars/media/create-media-browser/content-hierarchy)
- [Browse design guidelines](https://developers.google.com/cars/design/create-apps/media-apps/browsing-views#browsing_view_guidelines)
- [Media3 legacy root hint conversion](https://github.com/androidx/media/blob/1.11.1/libraries/session/src/main/java/androidx/media3/session/LegacyConversions.java)
- [Media3 legacy response size limit](https://github.com/androidx/media/blob/1.11.1/libraries/session/src/main/java/androidx/media3/session/MediaUtils.java#L44)
- [Media3 library callback contract](https://developer.android.com/reference/androidx/media3/session/MediaLibraryService.MediaLibrarySession.Callback)

---

## Phase 4 — Android Auto Browse Content Styles

Apply Android Auto's content-style hints to the Catalog from Phase 3. Set separate root defaults
for browsable and playable items in `LibraryParams.extras` on the `LibraryResult` returned by
`onGetLibraryRoot`. Use Media3's `MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE` and
`MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE`. Set both defaults to list so long Book,
Podcast, and Episode titles have room on the head unit. Category list styles are available for
browsable folders with tintable vector icons.

When a parent needs a different presentation, put the browsable or playable override in its
`MediaMetadata.extras`. A parent override applies to its immediate children. Use a single-item
override only when a row needs its own style, since it takes precedence over the parent and root
hints. Keep the media IDs, order, and browse limits from Phase 3.

Keep the title-range and publication-year browse folders from Phase 3. Defer inline group
headings until a browse screen needs them.

### Saved Progress and Download indicators

Add the saved indicators to `MediaMetadata.extras` for each Book and Episode, including entries
under Downloads. Media3 forwards these extras to the legacy browser item description.

- Map saved Progress to the host's unplayed, partially played, and fully played states. Include
  the saved completion fraction as a `Double` between `0.0` and `1.0` for partially played items.
- Derive Download status from the Phase 3 local availability checks. Mark a Book as downloaded
  once all its audio Tracks are available and an Episode once its audio file is available.
- Defer live progress-bar updates until after Phase 5 wires up playback. Live updates require the
  same media ID in the browse item, now-playing metadata, and playback state.

### Phase 4 acceptance

- Verify that the root defaults and any item overrides reach Android Auto through Media3's legacy
  browser bridge. Cover Books, Podcasts, Episodes, and both Downloads groups.
- Check lists in the Android Auto Desktop Head Unit with long titles, missing cover artwork, and
  folders with small icons. Check day and night modes and narrow and wide displays.
- Verify the saved indicators for unplayed, partially played, and finished content, a fully
  downloaded Book, a Book with missing Tracks, and an Episode whose audio file was removed.
- Verify that the title-range and publication-year folders still open and their children remain
  reachable.

### Documentation

- [Apply content styles](https://developer.android.com/training/cars/media/create-media-browser/content-styles)
- [Media3 1.11.1 content-style constants](https://github.com/androidx/media/blob/1.11.1/libraries/session/src/main/java/androidx/media3/session/MediaConstants.java#L142-L364)

---

## Phase 5 — Resolve Car Playback Requests

Selecting an Android Auto browse result generally sends ShelfDroid a media ID without a playable
URI. Media3 also conveys some voice playback requests through
`MediaItem.RequestMetadata.searchQuery`. Use `onAddMediaItems` to resolve individual requested
items, or `onSetMediaItems` when the request needs a Track list, start index, or saved position:

1. Parse the typed media ID or voice search query.
2. Validate that the selected Book or Episode still exists and is accessible to the current User.
3. Reuse the existing `PlayerRepository.playBook()` or `playPodcast()` preparation path so
   Progress, Downloads, preferences, and listening-session behavior stay consistent.
4. Reuse `MediaItemMapper` to produce playable items and the correct start index and position.
5. Complete the Media3 callback with those items so the session performs the requested player
   command. On failure, use the callback's supported error path and expose an actionable car-safe
   message; do not report success with an unresolved item.

Extract a small application-scoped playback coordinator if necessary so both phone UI requests and
car requests enter the same preparation pipeline. The current phone flow updates `PlayerStore` and
calls `playContent()`, which itself sets Media3 player items and starts playback. Define one owner
for that final player update: the car callback must not call `playContent()` and also return the
same items for Media3 to set. Keep `PlayerStore` UI state and session synchronization consistent
for both entry points. Do not make the callback navigate through `MainActivity` to start playback;
Android Auto must work when no Activity exists or can be shown.

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

### Documentation

- [Control playback with media sessions](https://developer.android.com/media/media3/session/control-playback)

---

## Phase 6 — Service Lifecycle and Error States

Android Auto can instantiate `PlaybackService` before ShelfDroid's Activity has ever opened. Test
and support all of these states explicitly:

- signed in with a warm Catalog cache
- signed in with an empty or stale cache
- signed out
- expired credentials or failed Session recovery
- Audiobookshelf server unavailable
- local-network server without the required permission
- downloaded content available while offline

When User action is required on the phone, show a localized message on the head unit telling the
User to look at their phone only when it is safe. Use this for sign-in and permission requests.
Never launch the phone UI automatically while driving.

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

### Documentation

- [Media3 background playback and resumption](https://developer.android.com/media/media3/session/background-playback)
- [Handle car media errors](https://developer.android.com/training/cars/media/errors)

---

## Phase 7 — Voice Playback, Browsable Search, and Playback Resumption

### Voice playback

Handle voice-initiated playback separately from the browsable search-results UI. Gemini and Google
Assistant can send a play-from-search request that Media3 represents as an incoming
`MediaItem` with `RequestMetadata.searchQuery`. Resolve that query through the playback coordinator
in `onAddMediaItems` or `onSetMediaItems`, matching Books, Podcasts, and Episodes by title and, where
useful, author. Handle an empty query as a general playback request by resuming the most recent
unfinished content or another appropriate current item. Voice-initiated playback is required before
public release.

### Browsable search results

Implement `onSearch` and `onGetSearchResult` so Android Auto can display selectable results. Once
the results are ready, call `notifySearchResultChanged()` with the requesting browser, query,
result count, and request parameters so the browser can fetch them through `onGetSearchResult`.
Grant `COMMAND_CODE_LIBRARY_SEARCH` and `COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT` to supported
browsers when this flow is ready. Media3 uses the search command to advertise support to legacy
browsers.

Search work must be asynchronous and bounded. Prefer indexed local Catalog queries rather than
scanning large in-memory collections or waiting for network search.

This overlaps with Part 1 of `docs/plan/google-assistant-integration.md`. The shared
`ShelfMediaLibraryCallback` and playback coordinator from this plan should own the common Media3
browse, voice, search-result, and playback-resolution behavior instead of adding more methods to
the anonymous callback in `PlayerModule`.

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

### Documentation

- [Support voice actions](https://developer.android.com/training/cars/media/voice-actions)
- [Media3 library callback contract](https://developer.android.com/reference/androidx/media3/session/MediaLibraryService.MediaLibrarySession.Callback)
- [Media3 background playback and resumption](https://developer.android.com/media/media3/session/background-playback)

---

## Phase 8 — Verification

### Automated tests

Add tests for:

- every typed media ID round trip
- malformed and stale IDs
- root child limits and ordering
- browsable versus playable metadata
- Book, Podcast, and Episode hierarchy mapping
- Books and Podcasts tab routing with zero, one, and multiple matching Libraries
- signed-out and offline results
- playback resolution for Books and Episodes
- local Download preference over remote playback
- voice-initiated playback for Book, Podcast, Episode, and empty queries
- browsable search ranking and results, when enabled
- custom-command availability for Android Auto controllers
- playback resumption state, when enabled

### Integration tests

Use the Media Controller Test app to verify the Media3 contract independently from Android Auto.
Use the Android Auto Desktop Head Unit for the complete experience.

Test at least:

1. `PlaybackService` starts before any Activity.
2. Force-stop ShelfDroid, then open it from Android Auto.
3. Clear app data, then open it from Android Auto.
4. Browse and play while signed in.
5. Browse while signed out and try accessing a local server with the required phone permission
   denied. Verify that the head unit tells the User to look at the phone only when it is safe.
6. Browse and play Downloads with the server unreachable.
7. Play, pause, seek back, seek forward, and use steering-wheel media buttons.
8. Switch audio focus between Android Auto and another source.
9. With both Gemini and Google Assistant, start a Book, Podcast, and Episode by voice and test a
   general playback request with no search terms.
10. Open and select browsable search results, when enabled.
11. Exercise day/night mode, touch, rotary input, and driving restrictions.
12. Verify artwork, text, and system tinting of white car icons in day and night mode on small,
    wide, and high-resolution DHU configurations.
13. Swipe ShelfDroid from phone recents while Android Auto playback is active.
14. Measure cold launch and content loading separately. Each must take no more than 10 seconds.
    App-specific buttons must respond within 2 seconds.

Run the existing relevant Gradle test suites and add the DHU result to the manual release checklist.

### Documentation

- [Test Android apps for cars](https://developer.android.com/training/cars/testing)
- [Test using the Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu)
- [Car app quality](https://developer.android.com/docs/quality-guidelines/car-app-quality)

---

## Phase 9 — Distribution

Before release:

1. Validate the Media category against the Android car app quality guidelines.
2. Upload an Android Auto-capable app bundle to a testing track.
3. In Play Console, open **Advanced settings → Form factors** and add Android Auto.
4. Provide review credentials under App Access because ShelfDroid requires an Audiobookshelf server
   and User sign-in.
5. Test through Internal App Sharing or an internal testing track before production review.

Internal App Sharing and internal testing skip car form-factor review. Closed testing gives
non-blocking review feedback. Car form-factor approval is required for open testing and production.

Android Auto is delivered as part of the existing mobile app. A dedicated Android Automotive OS
artifact and track are not required for this plan.

### Documentation

- [Distribute to cars](https://developer.android.com/training/cars/distribute)
- [Car app quality](https://developer.android.com/docs/quality-guidelines/car-app-quality)

---

## Recommended Commit Sequence

Use Conventional Commit titles in Commitizen format.

1. `feat(auto): declare Android Auto media capability`
2. `feat(auto): add typed car media identifiers`
3. `feat(auto): expose the cached catalog to media browsers`
4. `feat(auto): apply Android Auto browse content styles`
5. `feat(auto): resolve browser selections into playback`
6. `fix(media): support playback without an active phone task`
7. `feat(auto): support voice-initiated playback`
8. `feat(auto): add browsable search and playback resumption`
9. `test(auto): cover media browsing and playback flows`

Each commit should build and keep existing phone playback tests passing.

---

## Public Release Exit Criteria

Android Auto support is ready for public release when:

- Android Auto discovers ShelfDroid as a media app.
- The root tabs render from the cached Catalog and honor the browser's root-child limit.
- A User can browse and play a Book or Episode without opening the phone Activity.
- A User can start a Book, Podcast, or Episode with Gemini and Google Assistant. A general playback
  request starts appropriate current or recent content.
- play, pause, seek back, and seek forward work from the car.
- signed-out, offline, and unavailable-server states do not hang or crash the service.
- when sign-in or a permission requires phone interaction, the head unit tells the User to look at
  the phone only when it is safe.
- launch and content loading each take no more than 10 seconds. App-specific buttons respond within
  2 seconds. White car icons render with system tinting in day and night mode.
- active playback survives dismissal of the phone task.
- Media Controller Test and DHU verification pass.
- the implementation meets the applicable Media car quality requirements.

Displayed, selectable search results and playback resumption may follow the first public release.
Voice-initiated playback remains a public-release requirement even if displayed search results are
deferred.
