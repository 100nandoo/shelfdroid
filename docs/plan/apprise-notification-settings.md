# Apprise Notification Settings Findings

## Scope

This note captures the live Audiobookshelf Apprise notifications admin UI, the upstream client/server implementation, and the current ShelfDroid surface that would need to host it.

The live page inspected in Chrome DevTools MCP on July 25, 2026 was:

- `http://192.168.50.150:13378/audiobookshelf/config/notifications/`
- version badge: `v2.35.1`

## Resolved design decisions

The following decisions were resolved in the July 25, 2026 grilling pass for ShelfDroid:

- Canonical terms:
  - **Apprise notification settings**
  - **Notification rule**
  - **Notification event**
- ShelfDroid will implement this as a dedicated admin screen under the `Misc` admin cluster, not inside the current local sleep-timer screen.
- Access will mirror upstream intent: **Admin** and **Root** only.
- Non-admin users should not see the entry at all.
- The current local sleep-timer screen remains separate and unchanged in purpose.
- V1 does not require live socket-driven updates; reload after mutations is sufficient.
- Each **Notification rule** row should show:
  - event label
  - enabled state
  - destination summary
  - last fired / last failed state
  - consecutive failure count
  - test, edit, and delete actions
- Destination URLs should use the existing chip-input pattern rather than a multiline text field:
  - reference pattern: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/edititem/tabs/details/BookDetailsTab.kt:73-91`
  - reusable component: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/edititem/tabs/ChipInput.kt`
- Preserve `type` and `libraryId` in the data model, but do not expose them in the V1 UI.
- Client validation should require:
  - absolute global Apprise API URL
  - non-empty destination URLs
  - positive integers for queue size and max failed attempts
  - optional warning, not hard failure, when the global URL does not end with `/notify`
- V1 should expose per-rule test only; it should not expose the global `onTest` fire/fail controls.
- Related decision recorded in `docs/adr/0006-dedicated-admin-apprise-notification-settings-screen.md`.

## Live UI Findings

The live page is a dedicated admin notifications screen, not part of generic server settings.

- Header: `Apprise Notification Settings`
- Description explicitly says the global URL must be the full Apprise API endpoint, for example `http://host:8337/notify`
- Global settings form contains:
  - `Apprise API Url`
  - `Max queue size for notification events`
  - `Max failed attempts`
  - `Save`
- Notifications section contains:
  - `Create`
  - empty state: `No Notifications`
- The create modal opens on the same page and contains:
  - event selector
  - `Apprise URL(s)`
  - title template
  - body template
  - available-variable list
  - enabled toggle
  - submit action

The event menu shown in the live modal exposed six events:

- `onPodcastEpisodeDownloaded`
- `onBackupCompleted`
- `onBackupFailed`
- `onRSSFeedFailed`
- `onRSSFeedDisabled`
- `onTest`

Switching the modal event from `onTest` to `onPodcastEpisodeDownloaded` immediately changed the default title/body templates and the available variables list. Initial page load showed a single feature-specific request:

- `GET /api/notifications`

## Upstream Client Implementation

### Config page

Source: `audiobookshelf/client/pages/config/notifications.vue:3-173`

- Admin access is enforced in `asyncData`; non-admin users are redirected away (`:49-53`).
- Initial load uses `GET /api/notifications` and stores both `data` and `settings` (`:141-160`).
- Global settings save uses `PATCH /api/notifications` with:
  - `appriseApiUrl`
  - `maxNotificationQueue`
  - `maxFailedAttempts`
  (`:119-139`)
- The page listens for `notifications_updated` on the root socket and replaces local settings when events arrive (`:162-173`).

### Create/edit modal

Source: `audiobookshelf/client/components/modals/notification/NotificationEditModal.vue:1-197`

- The modal fields are:
  - event dropdown (`:11`)
  - `urls` multi-value input via `ui-multi-select` (`:13`)
  - title template (`:15`)
  - body template (`:17`)
  - available variables (`:19-21`)
  - enabled toggle (`:23-30`)
- Event changes overwrite the title/body draft from the selected event defaults (`:102-106`).
- New notifications default to:
  - `eventName = "onTest"`
  - `urls = []`
  - `enabled = true`
  - `type = null`
  (`:181-191`)
- Update uses `PATCH /api/notifications/:id` (`:125-146`).
- Create uses `POST /api/notifications` (`:147-166`).

### Notification cards

Source: `audiobookshelf/client/components/cards/NotificationCard.vue:1-171`

- Card styling changes based on `notification.enabled` (`:2`).
- The card title is the raw `eventName`, not a friendlier label (`:4`, `:42-44`).
- The card shows destination URLs joined into one line (`:17`).
- The card shows last-fired or last-failed status using `lastFiredAt`, `lastAttemptFailed`, and `numConsecutiveFailedAttempts` (`:19-20`, `:45-53`).
- Disabled cards show an `Enable` action that patches only `{ id, enabled: true }` (`:11`, `:120-137`).
- Edit and delete are per-card actions (`:13-14`, `:139-168`).

The two test flows are intentionally different:

- `onTest` cards use `GET /api/notifications/test?fail=0|1` (`:7-8`, `:63-77`)
- all other enabled cards use `GET /api/notifications/{id}/test` after a confirmation prompt (`:10`, `:92-119`)

## Upstream API and Server Behavior

### Route wiring

Source: `audiobookshelf/server/routers/ApiRouter.js:258-267`

- `GET /api/notifications`
- `PATCH /api/notifications`
- `GET /api/notificationdata`
- `GET /api/notifications/test`
- `POST /api/notifications`
- `DELETE /api/notifications/:id`
- `PATCH /api/notifications/:id`
- `GET /api/notifications/:id/test`

All routes are admin-only in practice because `NotificationController.middleware` enforces `req.user.isAdminOrUp` (`audiobookshelf/server/controllers/NotificationController.js:137-150`).

### Controller semantics

Source: `audiobookshelf/server/controllers/NotificationController.js:13-153`

- `GET /api/notifications` returns:
  - `data = NotificationManager.getData()`
  - `settings = Database.notificationSettings`
  (`:25-30`)
- `PATCH /api/notifications` updates global settings and returns `200` with no JSON body (`:38-44`).
- `POST /api/notifications` returns the current `Database.notificationSettings` object, whether or not a new rule was actually created (`:78-85`).
- `PATCH /api/notifications/:id` returns the current `Database.notificationSettings` object, even when the update body results in no change (`:106-112`).
- `DELETE /api/notifications/:id` returns the current `Database.notificationSettings` object (`:93-98`).
- `GET /api/notifications/:id/test` returns:
  - `400 "Apprise is not configured"` when the global API URL is missing (`:122-127`)
  - `200` on successful delivery
  - `500` on delivery failure
- If the `:id` route parameter does not match a rule, middleware returns `404` before the handler runs (`:142-147`).

### Settings and rule payload shape

Sources:

- `audiobookshelf/server/objects/settings/NotificationSettings.js:5-117`
- `audiobookshelf/server/objects/Notification.js:3-132`

`NotificationSettings.toJSON()` returns (`NotificationSettings.js:29-38`):

- `id`
- `appriseType`
- `appriseApiUrl`
- `notifications`
- `maxFailedAttempts`
- `maxNotificationQueue`
- `notificationDelay`

Each notification rule returns (`Notification.js:41-56`):

- `id`
- `libraryId`
- `eventName`
- `urls`
- `titleTemplate`
- `bodyTemplate`
- `enabled`
- `type`
- `lastFiredAt`
- `lastAttemptFailed`
- `numConsecutiveFailedAttempts`
- `numTimesFired`
- `createdAt`

### Event metadata

Source: `audiobookshelf/server/utils/notifications.js:3-110`

The canonical event catalog, descriptions, variables, defaults, and test data live entirely in `notificationData.events`.

Current events are:

- `onPodcastEpisodeDownloaded` (`:5-30`)
- `onBackupCompleted` (`:31-48`)
- `onBackupFailed` (`:49-62`)
- `onRSSFeedFailed` (`:63-78`)
- `onRSSFeedDisabled` (`:79-94`)
- `onTest` (`:95-108`)

This same metadata drives both:

- the page response from `GET /api/notifications`
- the modal defaults and variable display

### Delivery behavior

Source: `audiobookshelf/server/managers/NotificationManager.js:7-236`

- Delivery uses `axios.post(appriseApiUrl, payload, { timeout: 6000 })` (`:222-233`).
- The outbound Apprise request body is built by `Notification.getApprisePayload()` and contains only:
  - `urls`
  - `title`
  - `body`
  (`audiobookshelf/server/objects/Notification.js:125-130`)
- Notification `type` is stored in the rule model but is not included in the outbound Apprise payload.
- The manager serializes sends:
  - only one event is processed at a time (`NotificationManager.js:186-198`)
  - later events are queued up to `maxNotificationQueue` (`:187-193`)
  - queued events beyond that limit are dropped (`:188-190`)
  - the next event is delayed by `notificationDelay` (`:200-209`)
- Each triggered send updates rule runtime state (`:157-175`):
  - `lastFiredAt`
  - `lastAttemptFailed`
  - `numConsecutiveFailedAttempts`
  - `numTimesFired`
- A rule is auto-disabled once `numConsecutiveFailedAttempts >= maxFailedAttempts` (`:163-170`).
- After triggered sends, the server persists settings and emits `notifications_updated` over the socket (`:174-175`).

## Important Behavior Differences And Traps

### Global Apprise API URL vs per-rule destination URLs

The feature uses two different URL concepts:

- global `appriseApiUrl`: the Apprise API endpoint, expected to be the full `/notify` URL
- rule `urls`: the destination URLs sent inside the JSON body to the Apprise API

That split is central to the design and easy to confuse.

### The web UI validates more than the server

Client-side validation:

- `appriseApiUrl` must parse through `new URL(...)`
- `maxNotificationQueue > 0`
- `maxFailedAttempts > 0`
  (`audiobookshelf/client/pages/config/notifications.vue:83-117`)
- create/update requires at least one destination URL (`NotificationEditModal.vue:111-117`)

Server-side validation is much looser:

- global settings update only normalizes null/NaN-ish values; it does not validate URL format (`NotificationSettings.js:73-95`)
- create only checks that `eventName` exists and `urls.length` is non-zero (`:97-105`)
- update only checks that a matching notification object can be found from `payload.id` (`:107-116`)

That means ShelfDroid should not rely on the server to reject malformed input cleanly.

### Mutation endpoints are permissive

The controller does not return 4xx validation errors for most invalid bodies.

- `PATCH /api/notifications` always answers `200`
- `POST /api/notifications` answers `200` with current settings even when the payload is a no-op
- `PATCH /api/notifications/:id` answers `200` with current settings even when the payload is a no-op

For ShelfDroid, this means:

- validate before sending
- treat returned settings as the source of truth
- refetch after global-settings save because the patch endpoint is body-less

### The two test endpoints have different semantics

`GET /api/notifications/test?fail=0|1`

- only exercises enabled `onTest` rules
- goes through `triggerNotification()`
- updates runtime counters
- can intentionally increment failure counts with `fail=1`
- emits `notifications_updated`
- still returns `200` even when sends fail internally, and also returns `200` when Apprise is unconfigured because `triggerNotification()` exits early without throwing

`GET /api/notifications/:id/test`

- works for a specific rule regardless of event type
- returns `400` when Apprise is unconfigured
- returns `500` when the direct test send fails
- does not update runtime counters
- does not emit `notifications_updated`

For a mobile client, the per-rule test endpoint is the useful “did this send?” check. The global `onTest` endpoint is closer to a queue/failure-state exercise tool.

### OpenAPI docs are stale for this feature

The implementation is more trustworthy than the generated docs here.

Examples:

- `docs/objects/Notification.yaml:25` omits `onRSSFeedFailed` and `onRSSFeedDisabled`
- `docs/controllers/NotificationController.yaml` describes response shapes that do not match the controller methods
- `PATCH /api/notifications` is documented like a response-body endpoint, but the real controller returns only `200`

## ShelfDroid Current State

### Existing notification screen

Current ShelfDroid “notification settings” is still local sleep-timer prefs only.

- screen: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/settings/notification/SettingsNotificationScreen.kt:26-68`
- view model: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/settings/notification/SettingsNotificationViewModel.kt:15-35`
- repository: `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/settings/notification/SettingsNotificationRepository.kt:7-15`
- ui state: `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/settings/notification/SettingsNotificationUiState.kt:1-3`

Right now that screen only renders `SleepTimerSection` and writes to `PrefsRepository`.

### Best ShelfDroid reference pattern

The closest existing ShelfDroid implementation pattern is the admin email-management flow, not the current notification screen.

- API surface: `core-network/src/main/java/dev/halim/core/network/ApiService.kt:382-394`
- repository: `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/emailmanagement/EmailManagementRepository.kt:19-115`
- view model: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/emailmanagement/EmailManagementViewModel.kt:27-170`
- UI: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/emailmanagement/EmailManagementScreen.kt:58-394`

That flow already has the pieces Apprise management will need:

- initial remote load
- editable draft state
- loading and mutation states
- save/test actions
- create/edit/delete UI
- delete confirmation
- snackbar success/failure reporting

For destination editing specifically, ShelfDroid already has an existing chip-entry interaction pattern that should be reused instead of a newline-based text area:

- usage example: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/edititem/tabs/details/BookDetailsTab.kt:73-91`
- reusable component: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/edititem/tabs/ChipInput.kt`

### Admin gating detail

Upstream uses “admin or up”, not plain admin-only.

- upstream check: `audiobookshelf/server/controllers/NotificationController.js:137-150`
- current local helpers: `core/src/main/java/dev/halim/shelfdroid/core/User.kt:37-43`

ShelfDroid gating should include both `Admin` and `Root` semantics.

## ShelfDroid Implementation Takeaways

- Keep the current sleep-timer behavior intact.
- Add a separate admin-only **Apprise notification settings** screen under `Misc` rather than mixing remote state into the current simple prefs-only repository.
- Hide the screen entry from non-admin users instead of letting them discover a `403` path.
- Use `GET /api/notifications` as the single canonical bootstrap response.
- Model `PATCH /api/notifications` as a body-less success path and refetch after save.
- Preserve `type`, `libraryId`, and runtime counters in DTOs even if the first UI revision does not expose every field.
- Use the upstream event metadata from `data.events`; do not hard-code the event list in the app.
- Validate URL format and positive integers on the client, because the server is permissive.
- Prefer the per-rule `/api/notifications/{id}/test` endpoint for real delivery checks.
- Do not expose the global `onTest` fire/fail controls in V1.
- Reuse the existing `ChipInput` interaction for destination URLs instead of a multiline textarea.
- V1 can rely on reload-after-mutation rather than subscribing to `notifications_updated`.
- Do not assume upstream OpenAPI is accurate for this feature.

## Suggested Integration Points

- `core-network/src/main/java/dev/halim/core/network/ApiService.kt`
- new notification request/response DTOs under `core-network`
- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/settings/notification/`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/settings/notification/`

If the UI grows beyond a second section under the existing settings screen, the email-management screen structure is the better architectural template than the current sleep-timer implementation.
