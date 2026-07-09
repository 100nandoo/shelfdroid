# Open RSS Feed Item Findings

## Overview

This note documents the item-level `Open RSS Feed` flow in Audiobookshelf so ShelfDroid can implement the same feature without confusing it with the already-landed admin RSS feeds screen.

I verified the current UI live in Chrome MCP on July 8, 2026 at:

- `http://192.168.50.150:13378/audiobookshelf/item/77f04a6a-1a4c-4d81-9f1c-8bb79b07c24e`
- `http://192.168.50.150:13378/audiobookshelf/item/2bd337b1-4470-43a7-be65-692604c58b7e`

Related existing ShelfDroid note:

- `docs/plan/rss-feeds-upstream-findings.md` covers the separate admin `/config/rss-feeds` screen.

## What The Live UI Shows

- The current page is an item detail screen, not the admin RSS feeds list.
- The `Open RSS Feed` modal is currently in create mode, because the selected entity in page state has `feed = null`.
- The modal prefills the slug with the item id.
- The preview URL is shown immediately under the slug field.
- The modal exposes `RSS Details` and `Advanced` sections before submit.
- The `Prevent your feed from being indexed...` checkbox is checked by default.
- The HTTP warning is visible because this server is currently served over `http://`.
- The missing-pubDate warning is not showing for this item because the page state reports `hasEpisodesWithoutPubDate = false`.

Live verification:

- Chrome MCP accessibility snapshot taken on July 8, 2026
- Chrome MCP page-state evaluation of `store.state.globals.rssFeedEntity`

## Book-Specific Live Findings

- The book item page uses the same shared `Open RSS Feed` modal as the podcast item page.
- The current book page is `The Wonderful Wizard of Oz` and the modal is also in create mode with `feed = null`.
- The page-state payload still uses:
  - `type = "item"`
  - `feed = null`
  - `hasEpisodesWithoutPubDate = false`
- The modal copy is not specialized for books. It still says:
  - `Prevent your feed from being indexed by iTunes and Google podcast directories`
  - `Most podcast apps will require the RSS feed URL is using HTTPS`
- A prior successful request in this same browser session confirms that books open through the same endpoint and payload shape:
  - `POST /api/feeds/item/:itemId/open`
  - body: `serverAddress`, `slug`, `metadataDetails`
- The successful book-open response came back with `feed.entityType = "libraryItem"`, not `"item"`.
- The current book detail fetch also uses `include=downloads,rssfeed,share`, same as the podcast detail page.

Live verification:

- Chrome MCP accessibility snapshot for `/item/2bd337b1-4470-43a7-be65-692604c58b7e`
- Chrome MCP page-state evaluation for the book item modal
- Chrome MCP network request inspection for:
  - `GET /api/items/2bd337b1-4470-43a7-be65-692604c58b7e?expanded=1&include=downloads,rssfeed,share`
  - `POST /api/feeds/item/a28bf39b-3986-410f-b360-b11f2f970504/open`

## Web UI Implementation

- The item page fetches `GET /api/items/:id?expanded=1&include=downloads,rssfeed,share`, stores `item.rssFeed || null`, and only exposes the RSS action when the user is admin or a feed already exists, and the entity has episodes or tracks.
- Because the action gate checks both episodes and tracks, the same page-level flow applies to audiobooks as long as the item has audio tracks.
- Opening the action stores a small modal payload in global state:
  - `id`
  - `name`
  - `type = "item"`
  - `feed`
  - `hasEpisodesWithoutPubDate`
- The modal is a shared open-or-close component. If `currentFeed` exists it shows the existing feed URL and metadata plus a destructive `Close Feed` button. If `currentFeed` is null it shows the slug form and `Open Feed`.
- `init()` seeds `newFeedSlug` from `entityId`, so the default slug is the item id, not the title.
- `openFeed()` requires a non-empty slug, sanitizes it, and if the sanitized value differs it updates the text field and asks the user to submit again.
- The actual open call is:
  - `POST /api/feeds/item/:itemId/open`
  - payload: `serverAddress`, `slug`, `metadataDetails`
- `metadataDetails` includes:
  - `preventIndexing`
  - `ownerName`
  - `ownerEmail`

Source:

- `audiobookshelf/client/pages/item/_id/index.vue:394`
- `audiobookshelf/client/pages/item/_id/index.vue:636`
- `audiobookshelf/client/components/modals/rssfeed/OpenCloseModal.vue:38`
- `audiobookshelf/client/components/modals/rssfeed/OpenCloseModal.vue:85`
- `audiobookshelf/client/store/globals.js:12`
- `audiobookshelf/client/store/globals.js:149`

## Backend Contract

### Route surface

- Item open uses `POST /api/feeds/item/:itemId/open`.
- Feed close reuses `POST /api/feeds/:id/close`.
- Public delivery remains outside `/api` under `/feed/:slug...`.

Source:

- `audiobookshelf/server/routers/ApiRouter.js:305`
- `audiobookshelf/server/routers/ApiRouter.js:306`
- `audiobookshelf/server/Server.js:326`
- `audiobookshelf/server/Server.js:330`
- `audiobookshelf/server/Server.js:333`

### Controller behavior for item open

- The controller loads the expanded item and returns `404` when it does not exist.
- It checks that the current user can access the target library item.
- It rejects the request unless `serverAddress` and `slug` are both present strings.
- It rejects items with no audio tracks.
- It rejects slugs already in use.
- On success it returns `{ feed: feed.toOldJSONMinified() }`.

Source:

- `audiobookshelf/server/controllers/RSSFeedController.js:41`

### Manager behavior

- The open manager converts request options into feed options and defaults `preventIndexing` to `true` unless the payload explicitly sets it to `false`.
- Opening a feed emits `rss_feed_open` with the minified feed payload.
- Closing a feed emits `rss_feed_closed`.

Source:

- `audiobookshelf/server/managers/RssFeedManager.js:272`
- `audiobookshelf/server/managers/RssFeedManager.js:293`
- `audiobookshelf/server/managers/RssFeedManager.js:355`

### Minified response shape

The item-open response does not return the full admin feed object. The minified payload contains:

- `id`
- `entityType`
- `entityId`
- `feedUrl`
- `meta.title`
- `meta.description`
- `meta.preventIndexing`
- `meta.ownerName`
- `meta.ownerEmail`

Important nuance:

- The UI opens the modal with `type = "item"` in store state.
- The successful server response for a book returned `entityType = "libraryItem"`.
- That mismatch does not break the web UI because close uses `feed.id`, not the route type, but it is worth preserving in ShelfDroid models so the response is not over-normalized incorrectly.

Source:

- `audiobookshelf/server/models/Feed.js:665`

## Official Mobile App Precedent

- The official Audiobookshelf mobile app mirrors the same entity-scoped flow instead of exposing the admin list.
- It uses a fullscreen modal with the same two states:
  - open form when `currentFeed == null`
  - existing-feed view when `currentFeed != null`
- It posts the same payload shape to `/api/feeds/${entityType}/${entityId}/open`.
- It builds the full shareable URL as `serverAddress + currentFeed.feedUrl`.
- It uses the same global modal state pattern with `rssFeedEntity` and `showRSSFeedOpenCloseModal`.

Source:

- `audiobookshelf-app/components/modals/rssfeeds/RssFeedModal.vue:52`
- `audiobookshelf-app/components/modals/rssfeeds/RssFeedModal.vue:89`
- `audiobookshelf-app/store/globals.js:16`
- `audiobookshelf-app/store/globals.js:186`
- `audiobookshelf-app/components/modals/ItemMoreMenuModal.vue:456`

## ShelfDroid Current State

- ShelfDroid already has the separate admin RSS feeds list, detail sheet, copy action, and close action.
- The admin entry is wired from `Misc`, navigates through `RssFeeds`, and uses `GET /api/feeds` plus `POST /api/feeds/{feedId}/close`.
- ShelfDroid also already has podcast metadata editing for `edit_item_rss_feed_url`, but that field maps to the podcast source feed URL (`metadata.feedUrl`), not to the generated public Audiobookshelf feed created by `/api/feeds/.../open`.

Source:

- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:61`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/ShelfNavKey.kt:65`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/Navigation.kt:146`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/rssfeeds/RssFeedsScreen.kt:61`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/rssfeeds/RssFeedSheet.kt:89`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:141`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:386`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:388`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/edititem/tabs/details/PodcastDetailsTab.kt:58`
- `ShelfDroid/core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/edititem/EditItemMapper.kt:48`

## Concrete Gaps In ShelfDroid

- `ApiService.item(...)` only exposes `expanded=1`; it does not support the web client's `include=rssfeed` query.
- `LibraryItem` and `LibraryItemSerializer` currently ignore any top-level `rssFeed` field returned by the server.
- The item detail flows are cache-backed through `LibraryItemRepo.flowById(...)`, so item-level feed state cannot appear in the current UI until the cached item model grows a place to hold it.
- Current `/api/feeds` support in `ApiService` is limited to the admin list and close action. I did not find item-level open methods in the current network surface.
- If ShelfDroid wants parity for books as well as podcasts, the missing pieces are still the same shared item-level gaps rather than a separate book-only backend contract.

Source:

- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:141`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:386`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:388`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/response/LibraryItems.kt:23`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/response/libraryitem/LibraryItemSerializer.kt:25`
- `ShelfDroid/core-data/src/main/java/dev/halim/shelfdroid/core/data/response/LibraryItemRepo.kt:79`
- `ShelfDroid/core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/podcast/PodcastRepository.kt:38`

## Recommended ShelfDroid Scope

### Recommended first slice

Implement the shared item-level flow for both podcast and book item detail screens in the first slice.

Reasoning:

- The user-verified UI is an item screen.
- The live book screen uses the same modal and endpoint shape, so this does not need a separate backend design for books.
- The live podcast screen uses the same modal and endpoint shape.
- ShelfDroid already has the separate admin management screen.
- ShelfDroid already has dedicated item detail flows for both podcasts and books, while collection and series support would need more discovery and more model work.

### Suggested implementation shape

1. Extend item loading to support `include=rssfeed` on the item endpoint.
2. Add an optional top-level `rssFeed` model for item responses using the minified feed shape returned by Audiobookshelf.
3. Add `ApiService` support for `POST /api/feeds/item/{itemId}/open`.
4. Add a small request model containing:
   - `serverAddress`
   - `slug`
   - `metadataDetails.preventIndexing`
   - `metadataDetails.ownerName`
   - `metadataDetails.ownerEmail`
5. Surface a new item-level action from both podcast and book detail UI, not from the edit-metadata `rssFeedUrl` field. Put it in the podcast detail header beside `Add Episode` using the shared RSS icon, and in the book detail `PlayDownloadAndEdit` action row alongside the existing actions.
6. Keep the item-level model generic to `library item` rather than `podcast` because upstream uses the same flow for books with audio tracks.
7. Use a bottom sheet or dialog with the same two states as upstream:
   - open form when no generated feed exists
   - existing-feed view with copy and close when a feed exists
8. Prefill slug from the item id to match upstream.
9. Show the HTTPS warning when the configured server base URL is not HTTPS.
10. Reuse the existing copy affordance and strings where possible.

### Copy note

Upstream reuses podcast-oriented copy even on book items. If the goal is strict parity, ShelfDroid should keep one shared wording set first. If the goal is better Android UX, that wording can be generalized later without changing the network contract.

### Important design note

Do not merge this concept into ShelfDroid's existing `edit_item_rss_feed_url` field.

That field edits the source podcast feed URL stored in item metadata. The upstream `Open RSS Feed` modal manages a generated public Audiobookshelf feed for external podcast clients. They are related, but they are not the same object and should not share one input.

### Update strategy

Because ShelfDroid currently uses cache-backed item detail flows, the cleanest implementation is:

1. extend the item fetch and cached model first
2. call the open endpoint
3. update cached item state from the returned minified feed or from a follow-up refreshed item fetch

I would not block the first implementation on websocket parity with `rss_feed_open` / `rss_feed_closed`.

### Later slices

- Collection-level open flow: `POST /api/feeds/collection/{collectionId}/open`
- Series-level open flow: `POST /api/feeds/series/{seriesId}/open`
- Socket-driven live updates after open/close if ShelfDroid later wants the same push behavior as the web client and the official mobile app
