# Email Management Upstream Findings And ShelfDroid Plan

## Overview

This note documents how Audiobookshelf implements email management today so ShelfDroid can match the real feature instead of inventing a new shape.

I verified the current UI live in Chrome MCP on July 10, 2026 against an Audiobookshelf `v2.35.1` instance at `/config/email`, then checked the local `audiobookshelf` and `audiobookshelf-app` source.

## Where The Feature Lives

- Audiobookshelf exposes email management as a dedicated admin config page at `/config/email`.
- The page is separate from the generic `/config` settings form.
- The email feature actually spans three related surfaces:
  - admin SMTP settings plus shared e-reader device management at `/config/email`
  - user self-service e-reader device management on `/account`
  - item-level "Send Ebook To Device" actions in the official mobile app
- In ShelfDroid, the closest existing home for the admin page is the admin-only `Misc` screen, not the user `Settings` screen.

Source:

- `audiobookshelf/client/pages/config/email.vue:3`
- `audiobookshelf/client/pages/account.vue:76`
- `audiobookshelf-app/components/modals/ItemMoreMenuModal.vue:5`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:67`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/Navigation.kt:142`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/ShelfNavKey.kt:63`

## What The Admin Web UI Actually Does

### SMTP settings form

- The top card is a single form for:
  - `host`
  - `port`
  - `secure`
  - `rejectUnauthorized`
  - `user`
  - `pass`
  - `fromAddress`
  - `testAddress`
- The header includes a help link to the send-to-e-reader guide.
- The page loads settings once with `GET /api/emails/settings`.
- Save uses `PATCH /api/emails/settings`.
- Test uses `POST /api/emails/test`.
- The button behavior is deliberate:
  - `Save` is disabled unless the draft differs from the loaded settings.
  - `Reset` replaces `Test` whenever the draft is dirty.
  - `Test` only appears when there are no unsaved changes.
  - `Test` is disabled until `host` is non-empty.
- This gating matters because the test endpoint sends using persisted server settings, not the current unsaved form draft.
- The page shows a loading overlay during initial fetch.

Source:

- Live page snapshot from Chrome MCP on July 10, 2026
- `audiobookshelf/client/pages/config/email.vue:3`
- `audiobookshelf/client/pages/config/email.vue:12`
- `audiobookshelf/client/pages/config/email.vue:67`
- `audiobookshelf/client/pages/config/email.vue:154`
- `audiobookshelf/client/pages/config/email.vue:243`
- `audiobookshelf/client/pages/config/email.vue:270`
- `audiobookshelf/client/pages/config/email.vue:301`
- `audiobookshelf/server/controllers/EmailController.js:22`
- `audiobookshelf/server/controllers/EmailController.js:34`
- `audiobookshelf/server/controllers/EmailController.js:52`

### E-reader device section

- The second card manages e-reader devices with:
  - an `Add Device` action
  - a table of `Name`, `Email`, and `Accessible By`
  - edit and delete actions per row
  - an empty state when no devices exist
- The page does not patch individual devices. Create, edit, and delete all rebuild the full `ereaderDevices` array and post that array back to the server.
- If any device uses `specificUsers`, the page lazily loads `/api/users` and resolves the stored user IDs to usernames for display.

Source:

- Live page snapshot from Chrome MCP on July 10, 2026
- `audiobookshelf/client/pages/config/email.vue:79`
- `audiobookshelf/client/pages/config/email.vue:172`
- `audiobookshelf/client/pages/config/email.vue:187`
- `audiobookshelf/client/pages/config/email.vue:212`
- `audiobookshelf/client/pages/config/email.vue:230`

### Device editor modal

- Add and edit both use the same modal.
- The modal captures:
  - device `name`
  - device `email`
  - `availabilityOption`
  - `users` when `availabilityOption == "specificUsers"`
- Client-side validation blocks:
  - empty name
  - empty email
  - `specificUsers` with no selected users
  - duplicate device names
- The default availability is `adminOrUp`.
- Switching to `specificUsers` triggers a lazy user fetch if users have not already been loaded.

Source:

- `audiobookshelf/client/components/modals/emails/EReaderDeviceModal.vue:13`
- `audiobookshelf/client/components/modals/emails/EReaderDeviceModal.vue:21`
- `audiobookshelf/client/components/modals/emails/EReaderDeviceModal.vue:113`
- `audiobookshelf/client/components/modals/emails/EReaderDeviceModal.vue:123`
- `audiobookshelf/client/components/modals/emails/EReaderDeviceModal.vue:159`
- `audiobookshelf/client/components/modals/emails/EReaderDeviceModal.vue:187`
- `audiobookshelf/client/components/modals/emails/EReaderDeviceModal.vue:213`

## Backend Route Surface

### Admin routes

- `GET /api/emails/settings`
- `PATCH /api/emails/settings`
- `POST /api/emails/test`
- `POST /api/emails/ereader-devices`

These are guarded by `EmailController.adminMiddleware`, which requires `req.user.isAdminOrUp`.

### User and item routes

- `POST /api/emails/send-ebook-to-device`
- `POST /api/me/ereader-devices`

Important nuance:

- `send-ebook-to-device` is not behind the admin middleware.
- Access is enforced by runtime checks against:
  - e-reader device availability
  - the requesting user's access to the target library item
- `/api/me/ereader-devices` is the self-service path used by non-admin users with the `createEreader` permission.

Source:

- `audiobookshelf/server/routers/ApiRouter.js:272`
- `audiobookshelf/server/routers/ApiRouter.js:273`
- `audiobookshelf/server/routers/ApiRouter.js:274`
- `audiobookshelf/server/routers/ApiRouter.js:275`
- `audiobookshelf/server/routers/ApiRouter.js:276`
- `audiobookshelf/server/controllers/EmailController.js:96`
- `audiobookshelf/server/controllers/EmailController.js:133`
- `audiobookshelf/server/controllers/MeController.js:450`
- `audiobookshelf/client/pages/account.vue:127`
- `audiobookshelf/client/pages/account.vue:227`

## Settings And Send Semantics

### Email settings model

- The canonical settings object contains:
  - `id`
  - `host`
  - `port`
  - `secure`
  - `rejectUnauthorized`
  - `user`
  - `pass`
  - `testAddress`
  - `fromAddress`
  - `ereaderDevices`
- Defaults:
  - `port = 465`
  - `secure = true`
  - `rejectUnauthorized = true`
  - `ereaderDevices = []`
- `update()` coerces:
  - `port` to an integer, defaulting back to `465` when null/NaN
  - `secure` and `rejectUnauthorized` to booleans
- Device array normalization is opinionated:
  - non-arrays are ignored
  - invalid device rows are dropped
  - invalid availability values fall back to `adminOrUp`
  - `specificUsers` with no users also falls back to `adminOrUp`
  - non-`specificUsers` devices have `users` cleared

Source:

- `audiobookshelf/server/objects/settings/EmailSettings.js:14`
- `audiobookshelf/server/objects/settings/EmailSettings.js:50`
- `audiobookshelf/server/objects/settings/EmailSettings.js:65`
- `audiobookshelf/server/objects/settings/EmailSettings.js:77`

### SMTP transport behavior

- Audiobookshelf builds the Nodemailer transport from the saved settings.
- `secure` is only preserved when the port is `465`.
- When `rejectUnauthorized` is false, the transport sets `tls.rejectUnauthorized = false`.
- Both test-email sends and ebook sends verify the SMTP transporter before attempting delivery.
- A failed verification returns HTTP `400`.
- Test email delivery uses:
  - `from = fromAddress`
  - `to = testAddress || fromAddress`
- Ebook delivery uses:
  - `from = fromAddress`
  - `to = device.email`
  - one attachment using the server-side ebook file path

Source:

- `audiobookshelf/server/objects/settings/EmailSettings.js:114`
- `audiobookshelf/server/objects/settings/EmailSettings.js:130`
- `audiobookshelf/server/managers/EmailManager.js:8`
- `audiobookshelf/server/managers/EmailManager.js:12`
- `audiobookshelf/server/managers/EmailManager.js:25`
- `audiobookshelf/server/managers/EmailManager.js:39`
- `audiobookshelf/server/managers/EmailManager.js:52`

### Access checks for send-to-device

- `send-ebook-to-device` is only allowed when:
  - the named e-reader device exists
  - the current user can access that device
  - the target library item exists
  - the current user can access that library item
  - the item has an ebook file
- Device visibility is based on `availabilityOption`:
  - `adminOrUp`
  - `userOrUp`
  - `guestOrUp`
  - `specificUsers`

Source:

- `audiobookshelf/server/controllers/EmailController.js:96`
- `audiobookshelf/server/objects/settings/EmailSettings.js:146`
- `audiobookshelf/server/objects/settings/EmailSettings.js:164`
- `audiobookshelf/server/objects/settings/EmailSettings.js:174`

## Related User-Level Flow

- The web account page lets users manage their own personal e-reader devices when `user.permissions.createEreader` is enabled.
- That self-service page only shows devices where `users?.length == 1`.
- Mutations go to `POST /api/me/ereader-devices`, not the admin email settings endpoint.
- The server enforces that every device in this payload must be:
  - `availabilityOption == "specificUsers"`
  - owned by exactly the current user
- The official mobile app already consumes accessible e-reader devices for a send action in the item more-menu. It posts `libraryItemId` plus `deviceName` to `/api/emails/send-ebook-to-device`.

Source:

- `audiobookshelf/client/pages/account.vue:76`
- `audiobookshelf/client/pages/account.vue:127`
- `audiobookshelf/client/pages/account.vue:221`
- `audiobookshelf/server/controllers/MeController.js:455`
- `audiobookshelf-app/components/modals/ItemMoreMenuModal.vue:90`
- `audiobookshelf-app/components/modals/ItemMoreMenuModal.vue:156`
- `audiobookshelf-app/components/modals/ItemMoreMenuModal.vue:464`

## ShelfDroid Fit

### Existing navigation shape is a good match

- ShelfDroid already exposes singleton admin tools from the `Misc` screen.
- Existing admin routes already include:
  - RSS feeds
  - backups
  - logs
  - API keys
  - users
  - server settings
  - open sessions
  - listening sessions
- Email management belongs in that same cluster as a new dedicated admin route.

Source:

- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:67`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/Navigation.kt:142`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/ShelfNavKey.kt:63`

### Best local UI references

- `ServerSettingsScreen` is the best reference for:
  - top-level admin setting toggles
  - dirty-state save behavior
  - loading and snackbar handling
- `BackupsScreen` is the best reference for:
  - one screen that combines a settings section with a list below
  - mixed mutation types on one admin surface
- `ApiKeysScreen` is the best reference for:
  - list-based admin management
  - create/edit navigation patterns
  - post-edit refresh behavior

Source:

- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/serversettings/ServerSettingsScreen.kt:53`
- `ShelfDroid/core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/serversettings/ServerSettingsRepository.kt:28`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/backups/BackupsScreen.kt:107`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/apikeys/ApiKeysScreen.kt:41`

### Current gaps

- `ApiService` currently exposes `/api/settings`, `/api/logger-data`, and `/api/feeds`, but nothing for `/api/emails`.
- There are no email settings request or response models in `core-network`.
- ShelfDroid already carries the upstream `createEreader` permission in its login response, user update request, and edit-user UI, so the domain vocabulary is already aligned.
- ShelfDroid has a reusable single-select dropdown component, but no reusable multi-select user picker today.

Source:

- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:373`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:387`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/ApiService.kt:389`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/response/Login.kt:71`
- `ShelfDroid/core-network/src/main/java/dev/halim/core/network/request/UpdateUserRequest.kt:17`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/usersettings/edit/EditUserScreen.kt:321`
- `ShelfDroid/core-ui/src/main/java/dev/halim/shelfdroid/core/ui/components/DropDown.kt:37`

## Recommended ShelfDroid Implementation Shape

### Screen boundary

- Add a dedicated admin route, for example `EmailManagement`, under the `Misc` screen.
- Do not fold this into:
  - the user `Settings` screen
  - the existing `ServerSettingsScreen`
- Reasoning:
  - the upstream web client treats email as its own page
  - the feature spans SMTP settings plus device management
  - the API surface is `/api/emails/*`, not `/api/settings`

### Repository and UI state

- Do not reuse `ServerSettingsUiState`.
- Create a dedicated `EmailManagementUiState` with separate state for:
  - `savedSettings`
  - `draftSettings`
  - `devices`
  - `users`
  - `loading`
  - `apiState`
  - editor state for add/edit device flows
- Preserve the upstream button logic:
  - `Save` only when the draft differs from the last loaded server settings
  - `Reset` only when dirty
  - `Test` only when clean and `host` is nonblank
- This avoids the upstream footgun where `POST /api/emails/test` would otherwise test stale persisted settings while the user has unsaved edits on screen.

### API additions

Recommended first-pass `ApiService` additions:

- `emailSettings(): Result<EmailSettingsResponse>`
- `updateEmailSettings(request: UpdateEmailSettingsRequest): Result<EmailSettingsResponse>`
- `sendTestEmail(): Result<Unit>`
- `updateEreaderDevices(request: UpdateEreaderDevicesRequest): Result<EreaderDevicesResponse>`

Recommended follow-on additions:

- `updateMyEreaderDevices(request: UpdateEreaderDevicesRequest): Result<EreaderDevicesResponse>`
- `sendEbookToDevice(request: SendEbookToDeviceRequest): Result<Unit>`

Recommended models:

- `EmailSettings`
  - `id`
  - `host`
  - `port`
  - `secure`
  - `rejectUnauthorized`
  - `user`
  - `pass`
  - `testAddress`
  - `fromAddress`
  - `ereaderDevices`
- `EreaderDevice`
  - `name`
  - `email`
  - `availabilityOption`
  - `users`
- `EmailSettingsResponse(settings)`
- `EreaderDevicesResponse(ereaderDevices)`
- `UpdateEmailSettingsRequest(...)`
- `UpdateEreaderDevicesRequest(ereaderDevices)`

### UI composition

- Use one dedicated admin screen with two sections:
  - SMTP settings form
  - e-reader device list
- For the top form, copy the behavioral shape of `ServerSettingsScreen`, not the visual web layout.
- For the overall page composition, follow the `BackupsScreen` pattern of "settings block plus list below".
- For add/edit device, prefer a dedicated editor route or sheet over a tiny dialog:
  - ShelfDroid already has create/edit route patterns in API keys
  - the `specificUsers` case needs a real user-selection UI, not just a single dropdown

### Scope recommendation

Recommended first slice:

- admin-only `EmailManagement` screen
- `GET /api/emails/settings`
- `PATCH /api/emails/settings`
- `POST /api/emails/test`
- `POST /api/emails/ereader-devices`
- full support for all upstream availability modes, including `specificUsers`

Recommended second slice:

- load and cache accessible e-reader devices for the current user
- item-level send-to-device action for ebook items

Recommended third slice:

- user self-service e-reader device management via `/api/me/ereader-devices`

## Important Implementation Notes

- Device mutations are replace-all, not patch-one-item. Repository code should always build updates from the freshest known array and should trust the response body after a mutation.
- The admin endpoint emits `ereader-devices-updated`, and the user endpoint emits a user-scoped version of the same event, but ShelfDroid does not need socket support for the first version. A direct reload after mutation is simpler and safer.
- The live `GET /api/emails/settings` response I captured on July 10, 2026 returned the expected email settings structure, but the deployed instance omitted `testAddress` from that body while the upstream source model and page both include it. ShelfDroid should treat `testAddress` as supported.
- The web page currently triggers Chrome accessibility issues around form labeling and field metadata. ShelfDroid should not reproduce those weaknesses; Compose labels, content descriptions, password keyboard hints, and explicit error messages should be part of the implementation.
- SMTP password drafts should stay in screen memory only. There is no reason to persist them locally in datastores.

## Suggested Tests

- Repository tests for draft-vs-saved diffing and the `Test` button gating.
- Repository tests for replace-all device mutations so create, edit, and delete do not clobber unrelated devices.
- Serialization tests for `EmailSettings`, `EreaderDevice`, and `createEreader`-related flows.
- Navigation tests for the new `Misc` screen entry and route wiring.
- UI state tests for:
  - dirty form shows `Reset` instead of `Test`
  - `Save` enables only when changed
  - `specificUsers` requires at least one selected user
