# Download Module

## Table of Contents

- [Purpose](#purpose)
- [Architecture](#architecture)
- [Package Layout](#package-layout)
- [Main Components](#main-components)
- [External Collaborators](#external-collaborators)
- [Runtime Flows](#runtime-flows)
- [Notification Model](#notification-model)
- [State Model](#state-model)
- [Invariants](#invariants)
- [Testing and Gaps](#testing-and-gaps)
- [Recommended Documentation Pattern](#recommended-documentation-pattern)

## Purpose

The `download` Gradle module owns ShelfDroid's Media3-based offline download pipeline.

It is responsible for:

- enqueueing and removing Media3 downloads
- exposing app-facing download state to the rest of the codebase
- attaching semantic metadata to download requests so notifications and post-processing can recover intent
- rendering progress notifications for in-flight downloads
- materializing completed book and podcast playback downloads into durable shared storage

It is not responsible for:

- deciding when the UI should offer download actions
- choosing playback sources for books or podcasts
- managing Android's platform `DownloadManager` for non-playback file downloads
- syncing library metadata, progress, or sessions

Those responsibilities live in `core-data`, `core-ui`, `media`, and the managed-download flow outside this module.

## Architecture

At a high level, the module is a facade over Media3 download primitives with durable shared-storage export for books and podcasts:

```text
UI / repositories
  -> DownloadRepo
  -> Media3 DownloadService + DownloadManager
  -> notifications during transfer
  -> on playback completion: export cache -> shared storage
```

The most important design choice is that books and podcast episodes still download through Media3 first. ShelfDroid keeps Media3's existing progress and notification behavior, then exports completed playback media into shared storage so it survives app-data clear. That tradeoff is documented in ADR 0001.

## Package Layout

The module is now split by seam instead of keeping every adapter in one flat package:

- `dev.halim.shelfdroid.download`
  - module facade and state mapping
  - `DownloadRepo`
  - `DownloadMapper`
- `dev.halim.shelfdroid.download.notification`
  - notification payload and progress aggregation
  - `DownloadNotificationPayload`
  - `BookBatchProgressNotificationBuilder`
- `dev.halim.shelfdroid.download.service`
  - Media3 foreground service adapter
  - `ShelfDownloadService`
- `dev.halim.shelfdroid.download.storage`
  - shared readable-path policy
  - `ReadableStoragePolicy`
- `dev.halim.shelfdroid.download.storage.book`
  - durable book storage adapters
  - `BookFolderSelectionPolicy`
  - `BookDurableDownloadCatalog`
  - `BookDurableDownloadExporter`
- `dev.halim.shelfdroid.download.storage.podcast`
  - durable podcast storage adapters
  - `PodcastFolderSelectionPolicy`
  - `PodcastDurableDownloadCatalog`
  - `PodcastDurableDownloadExporter`

This layout improves locality: notification changes stay in `notification`, Media3 service wiring stays in `service`, and shared-storage rules stay under `storage`.

## Main Components

### `DownloadRepo`

File: `download/src/main/java/dev/halim/shelfdroid/download/DownloadRepo.kt`

This is the module facade used by `core-data` and `core-ui`.

It owns:

- enqueueing single downloads and book batch downloads
- podcast-specific enqueue/delete behavior
- translating Media3 `Download` entries into app `DownloadUiState`
- exposing reactive download invalidation signals
- interpreting durable shared files as the source of truth for completed playback downloads

The rest of the app should treat this class as the public API of the module.

### `ShelfDownloadService`

File: `download/src/main/java/dev/halim/shelfdroid/download/service/ShelfDownloadService.kt`

This is the Media3 `DownloadService` implementation.

It owns:

- running the foreground download service
- choosing the foreground notification content while downloads are active
- grouping visible progress text by logical download payload

It does not decide terminal notifications. That is delegated to `TerminalStateNotificationHelper` in the `media` module.

### `DownloadNotificationPayload`

File: `download/src/main/java/dev/halim/shelfdroid/download/notification/DownloadNotificationPayload.kt`

This class is the semantic contract attached to each `DownloadRequest`.

It carries:

- display title
- detail target id
- secondary label such as author or podcast title
- grouping metadata for book batches
- filename and readable-path metadata for durable shared-storage export

Without this payload, later stages would only see a raw Media3 download id and URL. The payload lets notifications and the exporter recover what kind of download completed and how it should be handled.

### `DownloadMapper`

File: `download/src/main/java/dev/halim/shelfdroid/download/DownloadMapper.kt`

This is a small adapter that maps Media3 download states into ShelfDroid's app-facing `DownloadState`.

It intentionally keeps the app insulated from raw Media3 constants.

### `BookBatchProgressNotificationBuilder`

File: `download/src/main/java/dev/halim/shelfdroid/download/notification/BookBatchProgressNotificationBuilder.kt`

Book downloads are special because one user action may enqueue many track downloads.

This builder:

- detects when the active foreground set represents exactly one book batch
- aggregates batch-wide progress from the download index
- renders a single custom notification body for that batch

If the current active downloads are not one book batch, the service falls back to the default grouped progress notification.

### `ReadableStoragePolicy`

File: `download/src/main/java/dev/halim/shelfdroid/download/storage/ReadableStoragePolicy.kt`

This class owns the durable playback path policy:

- folder root under public downloads
- `ShelfDroid/books/<title>_<author>/`
- `ShelfDroid/podcasts/<podcast-title>/`
- readable path sanitization rules

This keeps storage naming decisions out of the exporter and repositories.

### `BookDurableDownloadCatalog` and `PodcastDurableDownloadCatalog`

Files:

- `download/src/main/java/dev/halim/shelfdroid/download/storage/book/BookDurableDownloadCatalog.kt`
- `download/src/main/java/dev/halim/shelfdroid/download/storage/podcast/PodcastDurableDownloadCatalog.kt`

These are the shared-storage CRUD boundaries for durable playback files.

They own:

- exact-path lookup in `MediaStore.Downloads`
- best-effort scan fallback inside the `ShelfDroid` tree when exact lookup fails
- deleting an existing durable file or readable book folder contents
- writing a durable shared-storage file
- emitting a simple invalidation signal when durable files change

They do not know anything about Media3 cache internals or UI state. They only manage durable files in shared storage.

The book side also includes `BookFolderSelectionPolicy`, which keeps the readable folder stable when book metadata changes but the existing on-disk track set still matches the current Book. Recovery is intentionally strict: a Book only reuses an old folder when the exact expected folder exists or one candidate folder contains the full requested Track set. A partial single-folder match is not enough.

The podcast side now includes `PodcastFolderSelectionPolicy`, which follows the same philosophy for Episodes. Recovery first tries the exact readable path, then scans `ShelfDroid/podcasts/` for the requested filename, and only reattaches the local file when there is exactly one plausible match.

### `BookDurableDownloadExporter` and `PodcastDurableDownloadExporter`

Files:

- `download/src/main/java/dev/halim/shelfdroid/download/storage/book/BookDurableDownloadExporter.kt`
- `download/src/main/java/dev/halim/shelfdroid/download/storage/podcast/PodcastDurableDownloadExporter.kt`

These are the bridges from Media3 completion into durable shared storage.

They listen to `DownloadManager` changes and, for completed playback downloads:

- reads the semantic payload
- copies the completed bytes out of Media3 cache
- writes the file through the corresponding durable catalog
- removes the temporary Media3 cached resource
- for podcasts, removes the Media3 download entry afterward

This is what converts "completed Media3 cache entry" into "durable shared file that survives app-data clear".

## External Collaborators

The module depends on a few collaborators outside the module:

- `Helper` generates signed content URLs and open-detail intents
- `PlayerInternalStateHolder` and `PlayPauseControlStateHolder` together tell podcast re-download logic whether the target episode is currently active and currently playing
- `TerminalStateNotificationHelper` from the `media` module renders completed/failed notifications
- `DownloadModule` in the `media` module wires `DownloadManager`, `Cache`, and listeners together

These are important to mention because the download behavior is spread across module boundaries even though `DownloadRepo` remains the main entrypoint.

## Runtime Flows

### Single podcast episode download

1. A screen or repository asks `DownloadRepo` for a `DownloadUiState`.
2. The UI triggers `DownloadRepo.downloadPodcastEpisode(...)`.
3. `DownloadRepo` deletes any existing durable shared file for that episode before starting a new one.
4. If the same episode is currently playing from a recovered local file, destructive replacement is refused.
5. `DownloadRepo` enqueues a Media3 `DownloadRequest` with a `podcast_episode` payload.
6. `ShelfDownloadService` shows in-progress notifications using the payload metadata.
7. When Media3 marks the request completed, `PodcastDurableDownloadExporter` copies the cached bytes into `MediaStore.Downloads`.
8. The exporter removes the Media3 cache resource and the Media3 download entry.
9. Repositories recompute, and `DownloadRepo.item(...)` now reports the podcast episode as completed because the durable shared file exists.

### Book batch download

1. The UI triggers `DownloadRepo.downloadBook(...)`.
2. `DownloadRepo` resolves the readable destination folder, reusing an existing folder when metadata changed but the current track set still matches it.
3. Existing durable folder contents are deleted before the new Book download batch starts, unless that local Book is actively playing.
4. One Media3 request is enqueued per book track.
5. Each track carries batch metadata in `DownloadNotificationPayload`.
6. `ShelfDownloadService` and `BookBatchProgressNotificationBuilder` aggregate active progress into a single batch notification.
7. When each track completes, `BookDurableDownloadExporter` copies the cached bytes into the readable shared-storage folder.
8. `TerminalStateNotificationHelper` emits one final batch terminal notification after all tracks reach a terminal state.

Books use the durable shared files for offline recovery and playback.

### Delete flow

For books:

- `DownloadRepo.deleteBook(...)` removes durable shared files first
- then removes any matching Media3 download entries

For podcasts:

- `DownloadRepo.deletePodcastEpisode(...)` removes the durable shared file first
- then removes any remaining Media3 download entry

### Durable recovery after app-data clear

Recovery is lazy and on-demand. The app does not run a one-time startup migration over `MediaStore.Downloads`.

Instead:

1. Repositories recompute download state when screens load or when durable storage invalidation fires.
2. Book detail flows call `DownloadRepo.bookItem(...)` or `DownloadRepo.multipleTrackItem(...)`, which may recover durable Book files.
3. Podcast list and Episode detail flows call `DownloadRepo.item(...)`, which may recover a durable Episode file.
4. Playback startup calls `DownloadRepo.localBookTrackUris(...)` or `DownloadRepo.localPodcastEpisodeUri(...)` again, so recovery also happens when the user presses play.
5. Every recovery attempt tries the exact readable path first and only falls back to a best-effort scan if that lookup fails.
6. Books recover only when one candidate folder unambiguously contains the full requested Track set. Podcasts recover only when one candidate readable path unambiguously contains the requested filename.
7. If recovery remains ambiguous, the app leaves the `DownloadState` as unknown and does not attach a local playback URI to the wrong Library item.
8. If recovery succeeds, `PlaybackSessionResolver` keeps the resulting playback on the local-session path instead of opening a remote server session.

### State recomputation flow

Most callers do not read raw Media3 download values directly.

Instead, repositories combine:

- domain data such as item metadata and progress
- Media3 download invalidations
- durable playback storage invalidations

Then they call back into `DownloadRepo.item(...)` to compute the current `DownloadUiState`.

This is why some `combine(...)` calls include download-related flows even when their values are ignored. The flows are used as recomputation triggers, not as direct data inputs.

That same recomputation path is what triggers durable recovery after app-data clear. Recovery happens when the UI asks for current download state, not through a dedicated background scan.

## Notification Model

The notification system has two layers:

- foreground progress notification while transfers are active
- terminal completed/failed notification after transfers finish

Progress notifications come from `ShelfDownloadService`.

Terminal notifications come from `TerminalStateNotificationHelper`, which listens directly to `DownloadManager`.

The semantic glue between both layers is `DownloadNotificationPayload`.

## State Model

The app exposes download state through ShelfDroid's `DownloadState`, not Media3 enums.

Important distinction:

- for books and podcast episodes, the durable shared file is the completion signal

That distinction is deliberate. A Media3 entry that completed but has not yet been materialized into shared storage must not be treated as a durable offline playback file.

## Invariants

The module relies on a few rules that should remain true unless the design changes deliberately:

- user-visible download notification text must remain stable
- book durable files live under `Download/ShelfDroid/books/<title>_<author>/<server-filename>`
- podcast durable files live under `Download/ShelfDroid/podcasts/<podcast-title>/<server-filename>`
- playback download completion is defined by durable shared file existence
- starting a new podcast episode download may delete the previous durable file first
- destructive replacement must be refused while the target podcast episode is actively playing
- book and podcast download flows intentionally use different storage semantics

## Testing and Gaps

Current coverage in this module includes focused unit testing for readable path policy.

That now includes:

- book-folder ambiguity and full-track matching rules
- podcast rediscovery ambiguity rules
- player-side local-session routing when a recovered local file is present

Current gaps:

- on-device notification verification is still required for full acceptance of the durable shared-storage playback migration
- the durable export path itself is not unit tested in isolation
- cross-module integration behavior still relies mainly on compile verification and manual/runtime validation

## Recommended Documentation Pattern

For this module, the most useful documentation stack is:

- explanation doc like this one for architecture and intent
- ADRs for major tradeoffs and long-lived decisions
- targeted how-to docs only if contributors need repeated implementation guidance, such as adding a new download type

That is more maintainable here than trying to force a heavyweight documentation framework onto one Gradle module.
