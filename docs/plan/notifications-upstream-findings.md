# Notifications Upstream Findings

## Overview

This note documents how Audiobookshelf implements the admin notifications feature today so
ShelfDroid can follow the real backend and web UI contract instead of guessing from the screen.

## Where The Feature Lives

- The web app exposes notifications as a dedicated admin config page at `/config/notifications`, not
  as part of the generic `/config` settings form.
- The side-nav entry is defined in `audiobookshelf/client/components/app/ConfigSideNav.vue:90`.
- The page implementation lives in `audiobookshelf/client/pages/config/notifications.vue:1`.

## Admin Access Rules

- All notification routes are guarded by `NotificationController.middleware`.
- Access requires `req.user.isAdminOrUp`.
- If a route includes `:id`, the middleware also resolves the notification object and returns `404`
  when it does not exist.
- Source:
    - `audiobookshelf/server/controllers/NotificationController.js:137`
    - `audiobookshelf/server/routers/ApiRouter.js:260`

## Route Surface

Audiobookshelf registers these routes:

- `GET /api/notifications`
- `PATCH /api/notifications`
- `GET /api/notificationdata` (deprecated, older data-only route)
- `GET /api/notifications/test`
- `POST /api/notifications`
- `DELETE /api/notifications/:id`
- `PATCH /api/notifications/:id`
- `GET /api/notifications/:id/test`

Source:
´

- `audiobookshelf/server/routers/ApiRouter.js:260`

## Route Behavior

### `GET /api/notifications`

- Returns both notification metadata and persisted notification settings in one payload.
- Response shape:
    - `data: NotificationManager.getData()`
    - `settings: Database.notificationSettings`
- Source:
    - `audiobookshelf/server/controllers/NotificationController.js:25`

### `PATCH /api/notifications`

- Updates only the top-level Apprise settings.
- Persists changes through `Database.updateSetting(Database.notificationSettings)`.
- Returns `200` with an empty body.
- Source:
    - `audiobookshelf/server/controllers/NotificationController.js:38`

### `POST /api/notifications`

- Creates a notification entry from the submitted payload.
- Returns the full updated notification settings object.
- Source:
    - `audiobookshelf/server/controllers/NotificationController.js:78`

### `PATCH /api/notifications/:id`

- Updates the matched notification entry.
- Returns the full updated notification settings object.
- Source:
    - `audiobookshelf/server/controllers/NotificationController.js:106`

### `DELETE /api/notifications/:id`

- Deletes the matched notification entry.
- Returns the full updated notification settings object.
- Source:
    - `audiobookshelf/server/controllers/NotificationController.js:93`

### `GET /api/notifications/:id/test`

- Sends a test notification for one configured notification entry.
- Returns:
    - `400` when Apprise is not configured
    - `200` on success
    - `500` on send failure
- Source:
    - `audiobookshelf/server/controllers/NotificationController.js:122`

### `GET /api/notifications/test`

- Fires the synthetic `onTest` event through the notification manager.
- Supports `?fail=1` to intentionally fail the send path.
- Source:
    - `audiobookshelf/server/controllers/NotificationController.js:67`

## Persisted Settings Shape

The persisted top-level settings object is modeled by `NotificationSettings`.

Default values:

- `id = "notification-settings"`
- `appriseType = "api"`
- `appriseApiUrl = null`
- `notifications = []`
- `maxFailedAttempts = 5`
- `maxNotificationQueue = 20`
- `notificationDelay = 1000`

Important behavior:

- `isUseable` is true only when `appriseApiUrl` is configured.
- `update(payload)` only mutates:
    - `appriseApiUrl`
    - `maxFailedAttempts`
    - `maxNotificationQueue`
- `createNotification(payload)` requires:
    - `eventName`
    - `urls.length > 0`
- `updateNotification(payload)` delegates to the per-notification object.

Source:

- `audiobookshelf/server/objects/settings/NotificationSettings.js:5`

## Per-Notification Shape

Each notification entry stores:

- `id`
- `libraryId`
- `eventName`
- `urls`
- `titleTemplate`
- `bodyTemplate`
- `type`
- `enabled`
- `lastFiredAt`
- `lastAttemptFailed`
- `numConsecutiveFailedAttempts`
- `numTimesFired`
- `createdAt`

Important behavior:

- `setData(payload)` assigns a new UUID and stamps `createdAt = Date.now()`.
- Enabling a previously disabled notification resets failure tracking.
- `getApprisePayload(data)` converts the entry to:
    - `urls`
    - `title`
    - `body`
- Template variables use `{{ variableName }}` substitution.

Source:

- `audiobookshelf/server/objects/Notification.js:3`

## Event Metadata Returned To The Client

`NotificationManager.getData()` returns `notificationData`, which is defined in
`server/utils/notifications.js`.

Each event definition includes:

- `name`
- `requiresLibrary`
- `libraryMediaType` when applicable
- `description`
- `descriptionKey`
- `variables`
- `defaults.title`
- `defaults.body`
- `testData`

Current event list:

- `onPodcastEpisodeDownloaded`
- `onBackupCompleted`
- `onBackupFailed`
- `onRSSFeedFailed`
- `onRSSFeedDisabled`
- `onTest`

Source:

- `audiobookshelf/server/utils/notifications.js:3`

## Notification Manager Semantics

The backend notification manager adds behavior that matters for client expectations:

- It will not send anything unless `Database.notificationSettings.isUseable` is true.
- It filters configured notifications by `eventName` and `enabled`.
- It enforces a queue with `maxNotificationQueue`.
- It disables a notification automatically once `numConsecutiveFailedAttempts >= maxFailedAttempts`.
- It persists updated failure counters and timestamps after sends.
- It emits `notifications_updated` over the socket after firing notifications.
- It sends Apprise payloads with:
    - `urls`
    - `title`
    - `body`

Source:

- `audiobookshelf/server/managers/NotificationManager.js:7`

## Web UI Structure

The web page has two distinct sections.

### Top settings form

Fields:

- `Apprise API Url`
- `maxNotificationQueue`
- `maxFailedAttempts`

Behavior:

- Submit uses `PATCH /api/notifications`.
- Initial load uses `GET /api/notifications`.

Source:

- `audiobookshelf/client/pages/config/notifications.vue:3`

### Notification list and editor

List item behavior:

- `onTest` entries show `Fire On Test` and `Fire And Fail`.
- Enabled non-`onTest` entries show `Test`.
- Disabled entries show `Enable`.
- All entries show `Edit` and `Delete`.

Editor fields:

- event dropdown
- Apprise URLs
- title template
- body template
- enabled toggle

Important detail:

- The modal computes `showLibrarySelectInput`, but the template never renders a library selector.
- The backend supports `libraryId`, but the current web UI effectively ignores it.

Source:

- `audiobookshelf/client/components/cards/NotificationCard.vue:1`
- `audiobookshelf/client/components/modals/notification/NotificationEditModal.vue:1`

## Real-Time Updates

- The notifications page subscribes to the `notifications_updated` socket event.
- The page uses normal HTTP responses to update UI after CRUD actions.
- The socket is mainly relevant after backend-triggered send/test flows update timestamps and
  failure counts.

Source:

- `audiobookshelf/client/pages/config/notifications.vue:169`
- `audiobookshelf/server/managers/NotificationManager.js:178`

## ShelfDroid-Relevant Takeaways

- This feature is separate from `/api/settings`; it is not part of the existing server settings
  PATCH contract.
- The minimum backend contract ShelfDroid needs is:
    - `GET /api/notifications`
    - `PATCH /api/notifications`
    - `POST /api/notifications`
    - `PATCH /api/notifications/:id`
    - `DELETE /api/notifications/:id`
    - `GET /api/notifications/:id/test`
- `GET /api/notifications/test` is useful only if ShelfDroid wants parity with the special `onTest`
  helper buttons.
- A first ShelfDroid version can safely ignore `libraryId` selection because the upstream web UI
  does too.
