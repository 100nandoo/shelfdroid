# RSS Feeds Upstream Findings

## Overview

This note documents how Audiobookshelf implements RSS feed management today so ShelfDroid can match the real admin flow instead of guessing from the screen. I verified the current UI live in Chrome MCP on July 8, 2026 at `/config/rss-feeds`, then checked the upstream `audiobookshelf` and `audiobookshelf-app` source.

## Where The Feature Lives

- Audiobookshelf exposes RSS feeds as a dedicated admin config page at `/config/rss-feeds`.
- The side-nav entry is separate from the generic server settings form.
- In ShelfDroid, the closest existing home for this feature is the admin-only `Misc` screen, not the general `Settings` screen.

Source:

- `audiobookshelf/client/components/app/ConfigSideNav.vue:111`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:61`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:66`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:73`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:80`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:87`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:94`

## What The Web UI Actually Does

- The page loads once through `loadFeeds()`, which calls `GET /api/feeds`.
- The screen is read/list/view/close only. It does not create or open a feed from this page.
- Each row shows a cover thumbnail, title, slug on wide screens, entity type, episode count, prevent-indexing checkmark on wide screens, relative last-update text, and a close icon.
- Clicking a row opens a detail modal using the already-loaded feed object. There is no follow-up detail request.
- The detail modal shows the absolute feed URL, prevent-indexing, optional owner name/email, and a scrollable list of episode titles.
- The page sorts `feed.episodes` client-side by ascending `pubDate`, with null dates moved to the end.
- The list screen loads cover art from `feed.feedUrl + "/cover"`, not from `coverPath`.
- Closing a feed uses `POST /api/feeds/:id/close` and reloads the list on success.

Source:

- `audiobookshelf/client/pages/config/rss-feeds.vue:25`
- `audiobookshelf/client/pages/config/rss-feeds.vue:54`
- `audiobookshelf/client/pages/config/rss-feeds.vue:55`
- `audiobookshelf/client/pages/config/rss-feeds.vue:66`
- `audiobookshelf/client/pages/config/rss-feeds.vue:88`
- `audiobookshelf/client/pages/config/rss-feeds.vue:107`
- `audiobookshelf/client/pages/config/rss-feeds.vue:111`
- `audiobookshelf/client/pages/config/rss-feeds.vue:127`
- `audiobookshelf/client/pages/config/rss-feeds.vue:129`
- `audiobookshelf/client/pages/config/rss-feeds.vue:131`
- `audiobookshelf/client/pages/config/rss-feeds.vue:142`
- `audiobookshelf/client/pages/config/rss-feeds.vue:143`
- `audiobookshelf/client/pages/config/rss-feeds.vue:147`
- `audiobookshelf/client/components/modals/rssfeed/ViewFeedModal.vue:14`
- `audiobookshelf/client/components/modals/rssfeed/ViewFeedModal.vue:73`
- `audiobookshelf/client/components/modals/rssfeed/ViewFeedModal.vue:74`

## Backend Route Surface

Audiobookshelf registers these admin routes for feed management:

- `GET /api/feeds`
- `POST /api/feeds/item/:itemId/open`
- `POST /api/feeds/collection/:collectionId/open`
- `POST /api/feeds/series/:seriesId/open`
- `POST /api/feeds/:id/close`

The actual feed delivery routes are public and live outside `/api`:

- `GET /feed/:slug`
- `GET /feed/:slug/cover*`
- `GET /feed/:slug/item/:episodeId/*`

Source:

- `audiobookshelf/server/routers/ApiRouter.js:305`
- `audiobookshelf/server/routers/ApiRouter.js:306`
- `audiobookshelf/server/routers/ApiRouter.js:307`
- `audiobookshelf/server/routers/ApiRouter.js:308`
- `audiobookshelf/server/routers/ApiRouter.js:309`
- `audiobookshelf/server/Server.js:326`
- `audiobookshelf/server/Server.js:330`
- `audiobookshelf/server/Server.js:333`

## Admin Access Rules

- Every `/api/feeds` route is guarded by `RSSFeedController.middleware`.
- Access requires `req.user.isAdminOrUp`.

Source:

- `audiobookshelf/server/controllers/RSSFeedController.js:194`
- `audiobookshelf/server/controllers/RSSFeedController.js:195`

## Response Shape And Manager Semantics

### `GET /api/feeds`

- The controller returns both `feeds` and `minified`.
- The current web page consumes `feeds`, not `minified`.
- Full feed JSON includes:
  - `id`, `slug`, `userId`
  - `entityType`, `entityId`, `entityUpdatedAt`
  - `coverPath`, `serverAddress`, `feedUrl`
  - `meta`
  - `episodes`
  - `createdAt`, `updatedAt`
- `meta` includes:
  - `title`, `description`, `author`
  - `imageUrl`, `feedUrl`, `link`
  - `explicit`, `type`, `language`
  - `preventIndexing`, `ownerName`, `ownerEmail`
- `updatedAt`, `createdAt`, and `entityUpdatedAt` are numeric epoch-millis values in the JSON builder.
- `toOldJSONMinified()` only returns `id`, `entityType`, `entityId`, `feedUrl`, and a reduced `meta`.

Source:

- `audiobookshelf/server/controllers/RSSFeedController.js:25`
- `audiobookshelf/server/controllers/RSSFeedController.js:28`
- `audiobookshelf/server/controllers/RSSFeedController.js:29`
- `audiobookshelf/server/models/Feed.js:633`
- `audiobookshelf/server/models/Feed.js:641`
- `audiobookshelf/server/models/Feed.js:653`
- `audiobookshelf/server/models/Feed.js:654`
- `audiobookshelf/server/models/Feed.js:655`
- `audiobookshelf/server/models/Feed.js:659`
- `audiobookshelf/server/models/Feed.js:661`
- `audiobookshelf/server/models/Feed.js:665`
- `audiobookshelf/client/pages/config/rss-feeds.vue:131`

### Open And Close Behavior

- Close is `POST`, not `DELETE`.
- `handleCloseFeed()` removes the feed through `Database.feedModel.removeById(feed.id)` and emits `rss_feed_closed`.
- The open routes emit `rss_feed_open`.
- `preventIndexing` defaults to `true` unless the open payload explicitly sets it to `false`.

Source:

- `audiobookshelf/server/controllers/RSSFeedController.js:176`
- `audiobookshelf/server/managers/RssFeedManager.js:272`
- `audiobookshelf/server/managers/RssFeedManager.js:275`
- `audiobookshelf/server/managers/RssFeedManager.js:302`
- `audiobookshelf/server/managers/RssFeedManager.js:323`
- `audiobookshelf/server/managers/RssFeedManager.js:344`
- `audiobookshelf/server/managers/RssFeedManager.js:355`
- `audiobookshelf/server/managers/RssFeedManager.js:358`

### Freshness Caveat

- `GET /api/feeds` is just a persisted snapshot. It does not call the manager's update check first.
- On-demand freshness checks happen when the public feed itself is requested through `GET /feed/:slug`.
- That means the admin list can lag behind the latest entity changes until some code path refreshes the stored feed.

Source:

- `audiobookshelf/server/controllers/RSSFeedController.js:25`
- `audiobookshelf/server/managers/RssFeedManager.js:163`
- `audiobookshelf/server/managers/RssFeedManager.js:186`
- `audiobookshelf/server/managers/RssFeedManager.js:399`

## Audiobookshelf Mobile App Precedent

- The official Audiobookshelf mobile app code I checked does not expose a dedicated admin feed-management list screen.
- Its RSS support is item-scoped: a fullscreen modal can open a feed for the current entity or close an existing one.
- That modal builds the shareable URL from `serverAddress + currentFeed.feedUrl`.
- Item pages subscribe to `rss_feed_open` and `rss_feed_closed` so the item UI updates when feed state changes.

Source:

- `audiobookshelf-app/components/modals/rssfeeds/RssFeedModal.vue:95`
- `audiobookshelf-app/components/modals/rssfeeds/RssFeedModal.vue:119`
- `audiobookshelf-app/components/modals/rssfeeds/RssFeedModal.vue:122`
- `audiobookshelf-app/components/modals/rssfeeds/RssFeedModal.vue:152`
- `audiobookshelf-app/components/modals/rssfeeds/RssFeedModal.vue:170`
- `audiobookshelf-app/store/globals.js:16`
- `audiobookshelf-app/store/globals.js:184`
- `audiobookshelf-app/store/globals.js:188`
- `audiobookshelf-app/pages/item/_id/index.vue:746`
- `audiobookshelf-app/pages/item/_id/index.vue:747`
- `audiobookshelf-app/pages/item/_id/index.vue:793`
- `audiobookshelf-app/pages/item/_id/index.vue:794`

## ShelfDroid Fit

- ShelfDroid already routes admin tools from the home `Misc` screen and uses one nav key per singleton admin screen.
- Existing admin entries are `Backups`, `Logs`, `API Keys`, `Users`, and `Server Settings`.
- Admin screens follow a stable pattern:
  - nav key in `ShelfNavKey`
  - route wiring in `Navigation.kt`
  - `core-data/screen/<feature>` repository and UI state
  - `core-ui/screen/<feature>` screen plus ViewModel
- `ApiService.kt` already contains admin endpoints like backups, users, and logs, plus existing RSS-adjacent podcast feed lookup, but it has no `/api/feeds` methods today.
- String resources already include `copy_rss_url`, `rss_url_copied`, and `edit_item_rss_feed_url`, but not the admin-screen labels this feature needs.
- Coil `AsyncImage` support already exists, so cover thumbnails do not require new image infrastructure.

Source:

- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/ShelfNavKey.kt:63`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/Navigation.kt:144`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/apikeys/ApiKeysScreen.kt:41`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/apikeys/ApiKeysViewModel.kt:19`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/backups/BackupsScreen.kt:62`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/logs/LogsScreen.kt:58`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/serversettings/ServerSettingsScreen.kt:44`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/serversettings/ServerSettingsViewModel.kt:19`
- `ShelfDroid/core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/apikeys/ApiKeysRepository.kt:10`
- `ShelfDroid/core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/serversettings/ServerSettingsRepository.kt:9`
- `ShelfDroid/core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/logs/LogsRepository.kt:12`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:110`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:308`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:327`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:383`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/response/PodcastFeed.kt:6`
- `ShelfDroid/core-ui/src/main/res/values/strings.xml:289`
- `ShelfDroid/core-ui/src/main/res/values/strings.xml:290`
- `ShelfDroid/core-ui/src/main/res/values/strings.xml:397`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/components/Cover.kt:95`

## Recommended ShelfDroid Scope

### First slice

Implement the admin screen as read/list/view/close first. That matches the upstream web page exactly and avoids pulling item-level RSS creation into the same change.

### Concrete shape

1. Add an `RSS Feeds` admin entry beside the existing `Misc` admin actions.
2. Add a new nav key and route, following the same singleton-screen pattern as `ServerSettings`, `Logs`, and `Backups`.
3. Add `ApiService` methods for:
   - `GET /api/feeds`
   - `POST /api/feeds/{id}/close`
4. Add a new feature slice under `core-data/screen/rssfeeds` with:
   - `RssFeedsRepository`
   - `RssFeedsUiState`
   - `RssFeedsApiState`
   - a ViewModel similar to `ApiKeysViewModel`
5. Model only the fields the current UI actually uses:
   - row: `id`, `slug`, `entityType`, `updatedAt`, `feedUrl`, `serverAddress`
   - display: `meta.title`, `meta.preventIndexing`, `meta.ownerName`, `meta.ownerEmail`
   - cover: `feedUrl`
   - detail sheet: `episodes[].id`, `episodes[].title`, `episodes[].pubDate`
6. Use a mobile-native list plus detail sheet or dialog instead of trying to reproduce the desktop table. The desktop screen already hides several columns responsively, so pushing slug, full URL, prevent-indexing, owner fields, and episode titles into the detail view is the cleaner Android translation.
7. Build the absolute feed URL from the configured server base or `serverAddress + feedUrl`. Load the cover image from `feed.feedUrl + "/cover"`.
8. Keep a confirmation step before close. Upstream uses a confirm prompt in web and a dedicated destructive button in the mobile modal.

### Important non-goal for the first slice

Do not assume this screen creates feeds. Upstream feed creation is entity-scoped through the open endpoints, not part of the admin management page. If you want full RSS parity later, add a second slice for item/series/collection open-flow UI after the management screen lands.

### Behavioral note

If ShelfDroid wants to match upstream behavior closely, do not try to "refresh" feed contents client-side after every load. The current backend treats `/api/feeds` as persisted feed state, while `/feed/:slug` is the path that triggers on-demand recalculation.
