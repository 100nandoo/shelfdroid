# ShelfDroid Notifications Gap Analysis

## Overview

This note maps the upstream Audiobookshelf notifications feature onto the current ShelfDroid codebase and identifies the cleanest implementation shape for this repo.

## Current ShelfDroid State

### Admin entry points exist, but notifications is missing

- ShelfDroid already exposes an admin-only server management cluster from the home misc screen.
- Current entries:
  - Backups
  - Logs
  - API Keys
  - Users
  - Server Settings
  - Open Sessions
  - Listening Sessions
- There is no admin notifications entry yet.
- Source:
  - `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:47`
  - `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/Navigation.kt:122`
  - `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/ShelfNavKey.kt:17`

### ShelfDroid already has a different notification screen

- `Settings > Notification` already exists.
- That screen is for local player/device notification behavior, not server-side Apprise notifications.
- Reusing the same route or package name would create a naming collision and a UX collision.
- Source:
  - `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/settings/SettingsScreen.kt:42`
  - `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/settings/notification/SettingsNotificationScreen.kt:26`
  - `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/ShelfNavKey.kt:23`

### Current server settings code does not cover notifications

- The current server settings flow is backed by:
  - `authorize()`
  - `searchProviders()`
  - `updateSettings()`
  - `purgeCache()`
  - `purgeItemsCache()`
- It only maps fields from `serverSettings`.
- There is no notification-specific API call or UI state in this stack.
- Source:
  - `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/serversettings/ServerSettingsRepository.kt:9`
  - `core-network/src/main/java/dev/halim/core/network/ApiService.kt:369`
  - `core-network/src/main/java/dev/halim/core/network/response/Login.kt:83`

### The existing network layer has no `/api/notifications` contract

- `ApiService` currently has nothing for:
  - `GET /api/notifications`
  - `PATCH /api/notifications`
  - `POST /api/notifications`
  - `PATCH /api/notifications/:id`
  - `DELETE /api/notifications/:id`
  - `GET /api/notifications/:id/test`
- There are also no request or response models for notification settings, event metadata, or notification items.
- Source:
  - `core-network/src/main/java/dev/halim/core/network/ApiService.kt:1`
  - `core-network/src/main/java/dev/halim/core/network/request/`
  - `core-network/src/main/java/dev/halim/core/network/response/`

## Recommended Implementation Shape

### Separate admin screen, not an extension of `ServerSettingsScreen`

- Upstream Audiobookshelf treats notifications as a dedicated admin page.
- ShelfDroid already mirrors server admin features as separate screens under the misc menu.
- ShelfDroid should add a new admin route, for example `ServerNotifications`, instead of putting this into:
  - the existing user `Settings > Notification` screen
  - the current `ServerSettingsScreen`

Reasoning:

- It matches the upstream mental model.
- It avoids route and naming collisions with the existing local notification screen.
- It keeps the `/api/settings` feature set separate from `/api/notifications`.

### Reuse the Backups screen pattern

- The best existing local screen pattern is `BackupsScreen`.
- `BackupsScreen` already combines:
  - a top settings section
  - a list below
  - per-item actions
  - snackbar-driven async feedback
- Notifications has the same shape:
  - top Apprise settings form
  - list of configured notification entries
  - create/edit/delete/test actions

Source:

- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/backups/BackupsScreen.kt:107`

### Compose primitives already exist

ShelfDroid already has most of the UI building blocks needed.

- `ChipInput` for the list of Apprise URLs
  - `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/edititem/tabs/ChipInput.kt:1`
- `ChipDropdownMenu` for event selection
  - `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/components/DropDown.kt:37`
- `MyOutlinedTextField` and plain `OutlinedTextField` for editable templates and settings
  - `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/components/TextField.kt:79`
- `MyAlertDialog` for delete confirmation
  - `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/components/Dialog.kt:27`

### Data and API additions ShelfDroid will need

### Response models

ShelfDroid needs models for:

- top-level `GET /api/notifications` payload
  - `data`
  - `settings`
- notification settings
  - `appriseApiUrl`
  - `maxNotificationQueue`
  - `maxFailedAttempts`
  - `notifications`
- notification items
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
- notification event metadata
  - `name`
  - `requiresLibrary`
  - `libraryMediaType`
  - `description`
  - `descriptionKey`
  - `variables`
  - `defaults`

### Request models

ShelfDroid needs models for:

- top-level settings update
  - `appriseApiUrl`
  - `maxNotificationQueue`
  - `maxFailedAttempts`
- notification create/update
  - `id` for update flows
  - `libraryId`
  - `eventName`
  - `urls`
  - `titleTemplate`
  - `bodyTemplate`
  - `enabled`
  - `type`

### ApiService additions

Recommended additions:

- `notifications(): Result<NotificationSettingsResponse>`
- `updateNotificationSettings(...): Result<Unit>`
- `createNotification(...): Result<NotificationSettings>`
- `updateNotification(id, ...): Result<NotificationSettings>`
- `deleteNotification(id): Result<NotificationSettings>`
- `testNotification(id): Result<Unit>`
- optional `fireTestNotificationEvent(fail: Int = 0): Result<Unit>`

### UI Behavior ShelfDroid Should Preserve

### Minimum parity

- Load the page with `GET /api/notifications`.
- Show and save the top-level Apprise config.
- Show configured notifications.
- Create, edit, delete, and test individual notifications.
- Allow enable/disable as part of create/edit.

### Good parity

- Give `onTest` notifications a dedicated test action.
- Show last fired / last failed state from the server.
- Reload the list after tests so timestamps and failure counts are visible.

### Important Implementation Notes

### Do not rely on sockets for the first version

- The upstream web UI listens for `notifications_updated`.
- ShelfDroid does not currently consume that socket event for this feature.
- After a test action, ShelfDroid should explicitly reload notifications instead of waiting for a push update.

### `PATCH /api/notifications` returns no body

- The save-settings action cannot rely on a response payload the way `/api/settings` does.
- ShelfDroid should either:
  - update local saved state optimistically, or
  - reload notifications after a successful save

### `libraryId` can be deferred

- The backend and payload shape support `libraryId`.
- The current Audiobookshelf web modal computes library-related state but does not render a library selector.
- ShelfDroid can safely launch without a library picker and still match the effective upstream UI.

### This should not reuse `ServerSettingsUiState`

- `ServerSettingsUiState` is built around a compare-and-save flow for `/api/settings`.
- Notifications mixes:
  - top-level settings
  - list CRUD
  - test actions
  - event metadata
- A dedicated screen package and UI state will be cleaner than stretching the existing server settings classes.

### Suggested ShelfDroid File Layout

### Network

- `core-network/src/main/java/dev/halim/core/network/response/Notifications.kt`
- `core-network/src/main/java/dev/halim/core/network/request/UpdateNotificationSettingsRequest.kt`
- `core-network/src/main/java/dev/halim/core/network/request/UpsertNotificationRequest.kt`
- `core-network/src/main/java/dev/halim/core/network/ApiService.kt`

### Data

- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/servernotifications/ServerNotificationsRepository.kt`
- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/servernotifications/ServerNotificationsUiState.kt`

### UI

- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/servernotifications/ServerNotificationsScreen.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/servernotifications/ServerNotificationsViewModel.kt`
- optional supporting item/editor files if the screen is split up

### Navigation and menu

- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/ShelfNavKey.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/Navigation.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt`
- `core-ui/src/main/res/values/strings.xml`

### Practical Build Order

1. Add the network request and response contract.
2. Add repository and UI state with a working read-only load path.
3. Add the screen and admin navigation entry.
4. Add top-level settings save.
5. Add create/edit/delete flows.
6. Add per-notification test and explicit reload after test.
7. Only then consider extras like `onTest` fail helpers or socket syncing.
