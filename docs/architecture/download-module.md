# Download Module

## Purpose

The `download` Gradle module owns ShelfDroid's Media3-based offline download pipeline.

It is responsible for:

- enqueueing and removing Media3 downloads
- exposing app-facing download state to the rest of the codebase
- attaching semantic metadata to download requests so notifications and post-processing can recover intent
- rendering progress notifications for in-flight downloads
- materializing completed podcast episode downloads into durable shared storage

It is not responsible for:

- deciding when the UI should offer download actions
- choosing playback sources for books or podcasts
- managing Android's platform `DownloadManager` for non-playback file downloads
- syncing library metadata, progress, or sessions

Those responsibilities live in `core-data`, `core-ui`, `media`, and the managed-download flow outside this module.

## Architecture

At a high level, the module is a facade over Media3 download primitives with one podcast-specific durable-storage extension:

```text
UI / repositories
  -> DownloadRepo
  -> Media3 DownloadService + DownloadManager
  -> notifications during transfer
  -> on podcast completion: export cache -> shared storage
```

The most important design choice is that podcast episodes still download through Media3 first. ShelfDroid keeps Media3's existing progress and notification behavior, then exports completed podcast episodes into shared storage so they survive app-data clear. That tradeoff is documented in ADR 0001.

## Main Components

### `DownloadRepo`

File: `download/src/main/java/dev/halim/shelfdroid/download/DownloadRepo.kt`

This is the module facade used by `core-data` and `core-ui`.

It owns:

- enqueueing single downloads and book batch downloads
- podcast-specific enqueue/delete behavior
- translating Media3 `Download` entries into app `DownloadUiState`
- exposing reactive download invalidation signals
- interpreting durable podcast files as the source of truth for completed podcast downloads

The rest of the app should treat this class as the public API of the module.

### `ShelfDownloadService`

File: `download/src/main/java/dev/halim/shelfdroid/download/ShelfDownloadService.kt`

This is the Media3 `DownloadService` implementation.

It owns:

- running the foreground download service
- choosing the foreground notification content while downloads are active
- grouping visible progress text by logical download payload

It does not decide terminal notifications. That is delegated to `TerminalStateNotificationHelper` in the `media` module.

### `DownloadNotificationPayload`

File: `download/src/main/java/dev/halim/shelfdroid/download/DownloadNotificationPayload.kt`

This class is the semantic contract attached to each `DownloadRequest`.

It carries:

- display title
- detail target id
- secondary label such as author or podcast title
- grouping metadata for book batches
- filename metadata for durable podcast export

Without this payload, later stages would only see a raw Media3 download id and URL. The payload lets notifications and the exporter recover what kind of download completed and how it should be handled.

### `DownloadMapper`

File: `download/src/main/java/dev/halim/shelfdroid/download/DownloadMapper.kt`

This is a small adapter that maps Media3 download states into ShelfDroid's app-facing `DownloadState`.

It intentionally keeps the app insulated from raw Media3 constants.

### `BookBatchProgressNotificationBuilder`

File: `download/src/main/java/dev/halim/shelfdroid/download/BookBatchProgressNotificationBuilder.kt`

Book downloads are special because one user action may enqueue many track downloads.

This builder:

- detects when the active foreground set represents exactly one book batch
- aggregates batch-wide progress from the download index
- renders a single custom notification body for that batch

If the current active downloads are not one book batch, the service falls back to the default grouped progress notification.

### `ReadableStoragePolicy`

File: `download/src/main/java/dev/halim/shelfdroid/download/ReadableStoragePolicy.kt`

This class owns the durable podcast path policy:

- folder root under public downloads
- `ShelfDroid/podcasts/<podcast-title>/`
- readable path sanitization rules

This keeps storage naming decisions out of the exporter and repositories.

### `PodcastDurableDownloadCatalog`

File: `download/src/main/java/dev/halim/shelfdroid/download/PodcastDurableDownloadCatalog.kt`

This is the shared-storage CRUD boundary for durable podcast files.

It owns:

- exact-path lookup in `MediaStore.Downloads`
- deleting an existing durable podcast file
- writing a durable shared-storage file
- emitting a simple invalidation signal when durable files change

It does not know anything about Media3 cache internals or UI state. It only manages durable podcast files in shared storage.

### `PodcastDurableDownloadExporter`

File: `download/src/main/java/dev/halim/shelfdroid/download/PodcastDurableDownloadExporter.kt`

This is the bridge from Media3 completion into durable shared storage.

It listens to `DownloadManager` changes and, for completed podcast episode downloads:

- reads the semantic payload
- copies the completed bytes out of Media3 cache
- writes the file through `PodcastDurableDownloadCatalog`
- removes the temporary Media3 cached resource
- removes the Media3 download entry afterward

This is what converts "completed Media3 cache entry" into "durable shared file that survives app-data clear".

## External Collaborators

The module depends on a few collaborators outside the module:

- `Helper` generates signed content URLs and open-detail intents
- `PlayerInternalStateHolder` tells podcast re-download logic whether the target episode is currently playing
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
2. One Media3 request is enqueued per book track.
3. Each track carries batch metadata in `DownloadNotificationPayload`.
4. `ShelfDownloadService` and `BookBatchProgressNotificationBuilder` aggregate active progress into a single batch notification.
5. `TerminalStateNotificationHelper` emits one final batch terminal notification after all tracks reach a terminal state.

Books currently remain Media3-managed downloads. They do not use the durable shared-storage export path.

### Delete flow

For books:

- `DownloadRepo.delete(...)` removes the Media3 download entry.

For podcasts:

- `DownloadRepo.deletePodcastEpisode(...)` removes the durable shared file first
- then removes any remaining Media3 download entry

### State recomputation flow

Most callers do not read raw Media3 download values directly.

Instead, repositories combine:

- domain data such as item metadata and progress
- Media3 download invalidations
- durable podcast storage invalidations

Then they call back into `DownloadRepo.item(...)` to compute the current `DownloadUiState`.

This is why some `combine(...)` calls include download-related flows even when their values are ignored. The flows are used as recomputation triggers, not as direct data inputs.

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

- for books, a completed Media3 entry is the completion signal
- for podcast episodes, the durable shared file is the completion signal

That distinction is deliberate. A Media3 entry that completed but has not yet been materialized into shared storage must not be treated as a durable offline podcast file.

## Invariants

The module relies on a few rules that should remain true unless the design changes deliberately:

- user-visible download notification text must remain stable
- podcast durable files live under `Download/ShelfDroid/podcasts/<podcast-title>/<server-filename>`
- podcast download completion is defined by durable shared file existence
- starting a new podcast episode download may delete the previous durable file first
- destructive replacement must be refused while the target podcast episode is actively playing
- book and podcast download flows intentionally use different storage semantics

## Testing and Gaps

Current coverage in this module includes focused unit testing for readable path policy.

Current gaps:

- on-device notification verification is still required for full acceptance of the durable podcast migration
- the durable export path itself is not unit tested in isolation
- cross-module integration behavior still relies mainly on compile verification and manual/runtime validation

## Recommended Documentation Pattern

For this module, the most useful documentation stack is:

- explanation doc like this one for architecture and intent
- ADRs for major tradeoffs and long-lived decisions
- targeted how-to docs only if contributors need repeated implementation guidance, such as adding a new download type

That is more maintainable here than trying to force a heavyweight documentation framework onto one Gradle module.
