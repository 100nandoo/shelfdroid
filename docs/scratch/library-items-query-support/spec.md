# Library items query support

## Status

Pass 1 implemented. API contract support is in place; repository adoption is intentionally deferred.

## Goal

Bring ShelfDroid's `GET /api/libraries/{libraryId}/items` usage closer to the Audiobookshelf contract by supporting bounded pagination and the query parameters that matter for client behavior: `limit`, `page`, `minified`, `sort`, and `desc`.

No Audiobookshelf server change is required. This is a ShelfDroid client change.

For the first pass, keep current repository call sites unchanged. This pass is about contract support at the API boundary, not immediate adoption inside `LibraryItemRepository` or `HomeRepository`.

## Findings

1. ShelfDroid currently exposes only the path parameter for this endpoint.

   `ApiService.libraryItems()` is defined as:

   ```kotlin
   @GET("/api/libraries/{libraryId}/items")
   suspend fun libraryItems(@Path("libraryId") libraryId: String): Result<LibraryItemsResponse>
   ```

   Source: [ApiService.kt](../../core-network/src/main/java/dev/halim/core/network/ApiService.kt) (lines 158-159).

2. ShelfDroid's response model is already prepared for the richer Audiobookshelf payload.

   `LibraryItemsResponse` already models `total`, `limit`, `page`, `sortBy`, `sortDesc`, `filterBy`, `mediaType`, `minified`, `collapseseries`, and `include`.

   Source: [LibraryItems.kt](../../core-network/src/main/java/dev/halim/core/network/response/LibraryItems.kt) (lines 8-21).

3. The current sync path ignores pagination entirely and still loads the full item-id list in one request.

   `HomeRepository.remoteSync()` calls `libraryItemRepo.refreshLibraryItems()`, which calls `idsByLibraryId()`, which calls `api.libraryItems(libraryId)` once and maps `result.results` to IDs before calling `POST /api/items/batch/get`.

   Sources:

   - [HomeRepository.kt](../../core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/home/HomeRepository.kt) (lines 77-88)
   - [LibraryItemRepository.kt](../../core-data/src/main/java/dev/halim/shelfdroid/core/data/catalog/LibraryItemRepository.kt) (lines 51-77, 146-150)

4. Audiobookshelf's server endpoint supports the richer query contract already.

   `LibraryController.getLibraryItems()` reads:

   - `limit`
   - `page`
   - `sort`
   - `desc`
   - `filter`
   - `minified`
   - `collapseseries`
   - `include`

   It computes `offset = page * limit` and delegates to `Database.libraryItemModel.getByFilterAndSort(...)`.

   Sources in the Audiobookshelf repo checkout:

   - `server/routers/ApiRouter.js` (route `GET /libraries/:id/items`)
   - `server/controllers/LibraryController.js` (lines 604-639)
   - `server/utils/queries/libraryFilters.js` (lines 24-41)
   - `server/models/LibraryItem.js` (lines 294-328)

5. `minified` and `desc` must be sent as `1`/`0`, not `true`/`false`.

   Audiobookshelf checks `req.query.minified === '1'`, `req.query.desc === '1'`, and `req.query.collapseseries === '1'`. A Retrofit `Boolean` query parameter would serialize to `true`/`false`, which does not match that contract.

   Source in the Audiobookshelf repo checkout: `server/controllers/LibraryController.js` (lines 611-618).

6. The Audiobookshelf web app uses this endpoint in the way ShelfDroid should mirror for bounded loading.

   `LazyBookshelf.vue` builds requests like:

   ```text
   /api/libraries/{libraryId}/items
     ?limit={booksPerFetch}
     &page={page}
     &minified=1
     &include=rssfeed,numEpisodesIncomplete
     [&sort=...]
     [&desc=0|1]
     [&filter=...]
     [&collapseseries=1]
   ```

   It resets and reloads pages when sort/filter settings change.

   Source in the Audiobookshelf app repo checkout: `components/bookshelf/LazyBookshelf.vue` (notably lines 176-191 and 380-412).

7. The Audiobookshelf Android app uses only a narrow subset of the contract.

   Its `ApiHandler.getLibraryItems()` requests `?limit=100&minified=1`, and the author flow adds `filter=...`, `sort=media.metadata.title`, and `collapseseries=1`.

   Source in the Audiobookshelf app repo checkout: `android/app/src/main/java/com/audiobookshelf/app/server/ApiHandler.kt` (lines 500-512 and 562-581).

8. Sort support is not a one-to-one fit with ShelfDroid's current local display preferences.

   Audiobookshelf server sort keys for books include `addedAt`, `media.duration`, `media.metadata.title`, and `progress`, which map well to ShelfDroid's current book sorts. For podcasts, Audiobookshelf supports `addedAt`, `media.metadata.title`, `media.metadata.author`, `media.numTracks`, etc., but not podcast progress sorting on this endpoint.

   Sources in the Audiobookshelf repo checkout:

   - `server/utils/queries/libraryItemsBookFilters.js` (lines 254-301)
   - `server/utils/queries/libraryItemsPodcastFilters.js` (lines 78-103)

9. Adding pagination to `GET /api/libraries/{libraryId}/items` is necessary but not sufficient for the existing OOM issue.

   After collecting IDs, ShelfDroid still calls `POST /api/items/batch/get` with the full list. That remains an unbounded response risk and should still be chunked.

   Source: [library-items-response-oom/proposal.md](../library-items-response-oom/proposal.md).

## Gap summary

ShelfDroid is missing request-side support, not response-side support.

- The response DTO already matches Audiobookshelf.
- The Retrofit endpoint does not expose the query parameters.
- The repository sync flow assumes one unbounded response.
- The current home screen still sorts locally from a fully synchronized local catalog, so remote sort support should be added carefully and only used where it is actually representable.

## Proposed implementation

### Scoped rollout decision

The phrase "don't change the API call being called on repository for now" is interpreted here as:

- add support for the richer query contract at the Retrofit/API boundary now;
- do not change existing repository call sites in this pass;
- defer repository adoption of pagination/sort to a later pass.

This is a rollout decision, not a domain-model change. It does not require a glossary change or ADR by itself.

### 1. Extend `ApiService.libraryItems()`

Add query parameters to the Retrofit method.

Use integer flags for Audiobookshelf-compatible booleans:

```kotlin
@GET("/api/libraries/{libraryId}/items")
suspend fun libraryItems(
  @Path("libraryId") libraryId: String,
  @Query("limit") limit: Int? = null,
  @Query("page") page: Int? = null,
  @Query("minified") minified: Int? = null,
  @Query("sort") sort: LibraryItemsSort? = null,
  @Query("desc") desc: Int? = null,
): Result<LibraryItemsResponse>
```

Why this shape:

- `limit` and `page` match Audiobookshelf pagination directly.
- `minified` must be `1`/`0`.
- `sort` without `desc` is incomplete, so both should be added together.
- `null` keeps current call sites compatible until they opt in.

Primary file: [ApiService.kt](../../core-network/src/main/java/dev/halim/core/network/ApiService.kt).

### 2. Add a small query model/helper on the ShelfDroid side

Avoid threading five nullable parameters through repository code by introducing a local query object or helper conversion.

Recommended shape:

```kotlin
data class LibraryItemsQuery(
  val limit: Int? = null,
  val page: Int? = null,
  val minified: Boolean? = null,
  val sort: LibraryItemsSort? = null,
  val desc: Boolean? = null,
)
```

Use a typed sort model instead of raw strings.

Recommended shape:

```kotlin
sealed interface LibraryItemsSort {
  enum class Book(...) : LibraryItemsSort

  enum class Podcast(...) : LibraryItemsSort
}
```

This keeps book-only and podcast-only sort values distinct while still letting the API layer serialize the correct wire value.
In the current pass-1 implementation, that distinction is represented by separate enum namespaces under one shared `LibraryItemsSort` sealed interface. The future-call-site helper is typed; the low-level Retrofit boundary remains media-agnostic.

A mapper near the API boundary:

```kotlin
private fun Boolean?.toAudiobookshelfFlag(): Int? =
  this?.let { if (it) 1 else 0 }
```

Reason:

- keeps repository logic readable;
- encodes the `1`/`0` rule in one place;
- leaves room to add `filter`, `collapseseries`, or `include` later without expanding every call site again.

### 3. Do not change repository call sites in pass 1

For this pass, keep:

- `LibraryItemRepository.idsByLibraryId()` unchanged;
- `LibraryItemRepository.refreshLibraryItems()` unchanged;
- `HomeRepository.remoteSync()` unchanged.

Reason:

- this isolates the first change to contract support;
- it avoids mixing wire-contract expansion with sync-behavior changes;
- it gives a clean seam for later repository adoption and testing.

This also means pass 1 does not change runtime behavior yet.

### 4. Prepare repository adoption as pass 2

Once the contract surface exists, the next repository change should be:

- rewrite `idsByLibraryId()` to request `limit`, `page`, and `minified=1`;
- page until `total` is reached or an empty page is returned;
- keep the current cached-ID fallback on failure.

That later pass belongs primarily in [LibraryItemRepository.kt](../../core-data/src/main/java/dev/halim/shelfdroid/core/data/catalog/LibraryItemRepository.kt).

### 5. Keep remote sort support optional at the repository boundary

Add support for `sort` and `desc`, but do not force every current call site to use them immediately.

Recommended first use:

- sync/discovery path: `limit`, `page`, `minified`
- future lazy screen loading path: `limit`, `page`, `minified`, `sort`, `desc`

Reason:

- current ShelfDroid home screen still renders from the local database, not directly from paged network data;
- remote sort is most valuable once the UI itself becomes paginated/lazy;
- for podcast progress sorting, the remote endpoint does not offer a matching sort key.

### 6. Centralize sort-key mapping instead of scattering raw strings

If ShelfDroid later uses remote sort for screen loading, keep the mapping in one place.

Safe initial mapping:

| ShelfDroid concept | Audiobookshelf sort key |
| --- | --- |
| Book `AddedAt` | `addedAt` |
| Book `Duration` | `media.duration` |
| Book `Title` | `media.metadata.title` |
| Book `Progress` | `progress` |
| Podcast `AddedAt` | `addedAt` |
| Podcast `Title` | `media.metadata.title` |
| Podcast `Progress` | unsupported remotely |

The unsupported podcast-progress case should remain client-side unless ShelfDroid changes behavior intentionally.

### 7. Do not treat pass 1 as a sync fix

Because pass 1 does not change repository call sites, it does not change the current OOM behavior by itself.

The later repository-adoption pass should still be paired with the already-identified batching work for `POST /api/items/batch/get`.

Otherwise:

- first pass adds contract support only;
- second pass would fix the first unbounded call;
- the batch endpoint would still remain the second unbounded risk until chunked.

Related doc: [library-items-response-oom/proposal.md](../library-items-response-oom/proposal.md).

## Suggested file-level plan

1. `core-network/src/main/java/dev/halim/core/network/ApiService.kt`
   - extend `libraryItems()` with query parameters

2. `core-network/src/main/java/dev/halim/core/network/LibraryItemsQuery.kt`
   - typed query model
   - typed sort model
   - boolean flag conversion
   - future call-site helper overload

3. Keep all current repository call sites unchanged in the first pass
   - especially for podcast progress sort

4. Later repository-adoption pass
   - update `LibraryItemRepository.idsByLibraryId()` to paginate
   - update related sync tests

5. Separate OOM-hardening pass
   - chunk `POST /api/items/batch/get`

## Test plan

1. Network request coverage

   - verify `limit`, `page`, `sort` are serialized correctly
   - verify `minified=true` becomes `minified=1`
   - verify `desc=true` becomes `desc=1`

2. Repository pagination coverage

   Defer to the repository-adoption pass:

   - multiple pages accumulate IDs correctly
   - empty page terminates the loop
   - failure falls back to cached local IDs

3. Regression coverage

   - no behavior change for existing repository call sites
   - no behavior change for existing callers that do not pass query params
   - podcast-progress sort remains client-side unless explicitly redesigned

## Recommendation

Implement this in three passes.

Current state:

1. pass 1 is implemented: `ApiService` supports typed query parameters and `LibraryItemsQuery`;
2. pass 2 is still pending: repository adoption for paginated `idsByLibraryId()`;
3. pass 3 is still pending: direct UI use of remote pagination/sort if chosen.

That keeps the first change small, respects the current repository boundary, and avoids mixing endpoint parity with sync-behavior changes or the larger home-screen loading redesign.
