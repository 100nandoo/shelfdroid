# Authentication Management Findings and Implementation Plan

## Status and provenance

This document is a research and implementation plan, not an implementation.

- Browser inspection: Audiobookshelf v2.36.0 Authentication page at
  `/config/authentication`, inspected with Chrome DevTools MCP on 2026-08-18.
- Server source: local Audiobookshelf checkout at `/Users/fernando/Codes/audiobookshelf`,
  commit `9b92b5de` from 2026-06-20.
- Client source: current ShelfDroid checkout at the time this document was written.
- Sensitive values observed in the browser and API responses are intentionally omitted.

## Decision summary

ShelfDroid should add a dedicated **Authentication** management screen under the existing
admin-only `Misc` server section. The feature should manage only the Audiobookshelf server's
login configuration exposed by `/api/auth-settings`. Existing User account CRUD remains a
separate feature.

Administrative access means an Audiobookshelf `admin` or `root` user. It does not mean a regular
user who has individual `update`, `delete`, or `upload` permissions. ShelfDroid already maps both
`admin` and `root` to `UserPrefs.isAdmin`; the new feature should use that existing app-level flag.

Access must be enforced in three places:

- Hide the Authentication entry for non-admin users.
- Refuse to load the destination before making a secret-bearing API call when local user state is
  not admin.
- Handle the Audiobookshelf server's `403` as the authoritative denial when local state is stale.

The OIDC client secret must never be persisted or shown after loading. ShelfDroid should map the
raw response to a boolean "configured" state, discard the raw value, and send a secret only when
the admin explicitly replaces or clears it.

## Scope

Included:

- Custom login message.
- Password sign-in enablement.
- OpenID login enablement and OIDC configuration.
- OIDC issuer discovery for auto-populating provider endpoints and signing algorithms.
- Mobile redirect URI allowlist management.
- Existing-user matching, automatic launch, automatic registration, group claim, and advanced
  permissions claim.
- Admin/root authorization, validation, restart messaging, and secret handling.

Not included:

- User creation, editing, deletion, locking, or OIDC unlinking. ShelfDroid already has a separate
  Users management surface.
- Authentication provider setup outside Audiobookshelf.
- Restarting the Audiobookshelf server from ShelfDroid.
- Changing ShelfDroid's existing OIDC callback scheme.
- Screenshot and recent-app-preview protection, which is explicitly deferred.

## Browser UI findings

The Audiobookshelf v2.36.0 web page uses one vertically scrolling admin form with three card-like
sections and a single Save action.

| Section | Controls | Behavior |
| --- | --- | --- |
| Custom message | Enable checkbox and rich-text editor | The checkbox is derived from whether `authLoginCustomMessage` is non-empty. There is no separate server flag. |
| Password sign-in | Enable checkbox | Adds or removes `local` from `authActiveAuthMethods`. |
| OpenID login | Enable checkbox and OIDC fields | Adds or removes `openid` from `authActiveAuthMethods`; the detailed form is shown when enabled. |

The OIDC form exposes:

- Issuer URL and an Auto-populate action.
- Authorization, token, userinfo, JWKS, and logout URLs.
- Client ID and client secret.
- Signing algorithm.
- Allowed mobile redirect URIs.
- Callback subfolder selection and calculated callback URLs.
- Login button text.
- Existing-user match strategy: none, email, or username.
- Automatic launch and automatic registration.
- Group claim and advanced permissions claim.
- A read-only sample advanced-permissions JSON object.
- A warning that the server must be restarted after OIDC changes.

The desktop web layout is not appropriate to copy literally into a phone UI. ShelfDroid should
keep the same concepts but use compact Material 3 sections, clear field grouping, inline
validation, and a sticky or consistently reachable Save action.

Accessibility issues reported by Chrome on the reference web page include unlabeled form fields
and fields without `id` or `name`. ShelfDroid should not reproduce those issues: every field and
toggle must have a visible label, supporting text where needed, and meaningful TalkBack semantics.

Reference:

- `client/pages/config/authentication.vue:1-129`
- `client/pages/config/authentication.vue:254-341`

## Observed API calls

Opening the page produced these relevant authenticated calls:

| Method and path | Purpose | Browser result |
| --- | --- | --- |
| `POST /api/authorize` | Refresh the current user and normal browser-safe server settings | `200` |
| `GET /api/auth-settings` | Load the complete editable authentication configuration | `304` from cache, with the full response available |

The dedicated authentication response includes the raw OIDC client secret. This is intentionally
different from normal browser-safe server settings, which remove the client ID, client secret,
redirect allowlist, and claim fields.

No save request was issued during inspection because that would mutate the server. The endpoint
and payload behavior below were confirmed from the Audiobookshelf server and web client source.

## Audiobookshelf API contract

### Authorization

All `/api` routes pass JWT authentication. Authentication settings routes additionally require
`user.isAdminOrUp`, which is true only for `root` or `admin`. Other users receive `403`.

Reference:

- `server/Server.js:318`
- `server/controllers/MiscController.js:615-733`
- `server/models/User.js:538-543`

### Read settings

```http
GET /api/auth-settings
Authorization: Bearer <access-token>
```

The response is the complete editable authentication settings object. Relevant fields are:

| JSON field | Type | Notes |
| --- | --- | --- |
| `authLoginCustomMessage` | string or null | HTML used on the login page. Null means disabled. |
| `authActiveAuthMethods` | string array | Supported values are `local` and `openid`; at least one must remain. |
| `authOpenIDIssuerURL` | string or null | OIDC issuer. |
| `authOpenIDAuthorizationURL` | string or null | Authorization endpoint. |
| `authOpenIDTokenURL` | string or null | Token endpoint. |
| `authOpenIDUserInfoURL` | string or null | Userinfo endpoint. |
| `authOpenIDJwksURL` | string or null | JWKS endpoint. |
| `authOpenIDLogoutURL` | string or null | Optional provider logout endpoint. |
| `authOpenIDClientID` | string or null | Sensitive configuration but not treated as a secret. |
| `authOpenIDClientSecret` | string or null | Returned as raw text; must be discarded after mapping. |
| `authOpenIDTokenSigningAlgorithm` | string or null | Defaults to `RS256`; discovery may supply valid choices. |
| `authOpenIDMobileRedirectURIs` | string array | Allowlist; `*` is permitted only as the sole entry. |
| `authOpenIDSubfolderForRedirectURLs` | string | Empty string means no callback subfolder. |
| `authOpenIDButtonText` | string or null | Login button label. |
| `authOpenIDMatchExistingBy` | string or null | UI-supported values are `email`, `username`, or null. |
| `authOpenIDAutoLaunch` | boolean | Automatically starts OpenID login from the login page. |
| `authOpenIDAutoRegister` | boolean | Creates a user after a successful unmatched OpenID login. |
| `authOpenIDGroupClaim` | string or null | Claim containing `admin`, `user`, or `guest` group membership. |
| `authOpenIDAdvancedPermsClaim` | string or null | Claim containing non-admin permission data. |
| `authOpenIDSamplePermissions` | string | Read-only example JSON for the advanced permissions claim. |

Reference:

- `server/objects/settings/ServerSettings.js:64-85`
- `server/objects/settings/ServerSettings.js:263-319`

### Update settings

```http
PATCH /api/auth-settings
Authorization: Bearer <access-token>
Content-Type: application/json
```

The endpoint is a real partial PATCH even though the Audiobookshelf web client sends the whole
form. Omitted known fields remain unchanged, unknown fields are ignored, and `{}` is accepted as a
no-op. ShelfDroid should send only changed fields to reduce accidental secret and configuration
overwrites.

Successful update:

```json
{
  "updated": true,
  "serverSettings": {}
}
```

No-op or skipped-invalid update:

```json
{
  "updated": false,
  "serverSettings": {}
}
```

The actual `serverSettings` object contains normal browser-safe settings, not the authentication
fields required to rebuild the form. After an update, ShelfDroid should perform a fresh
`GET /api/auth-settings` and use that canonical response as the new saved snapshot.

Important PATCH behavior:

- Omitted property preserves the current value.
- Empty string clears string fields by canonicalizing them to null.
- `authOpenIDSubfolderForRedirectURLs` is the exception; empty string is preserved and means no
  subfolder.
- Invalid known values are usually skipped rather than returning `400`.
- A non-object body returns `400`.
- A non-admin/root user receives `403`.
- `authActiveAuthMethods` is filtered to `local` and `openid`, and an empty result is ignored.
- Redirect URIs must be strings matching the server's URI pattern; `*` must be the only element.
- Auto-launch and auto-register must be actual JSON booleans.
- Other editable fields must be a string or null.

ShelfDroid's shared JSON configuration uses `explicitNulls = false`, so null request properties are
omitted. To explicitly clear a string field, the request mapper must send `""`, not null. This
distinguishes clear from preserve without changing the app-wide serializer.

Reference:

- `server/controllers/MiscController.js:629-733`
- `client/pages/config/authentication.vue:325-370`
- `core-network/src/main/java/dev/halim/core/network/di/NetworkModule.kt:25-35`

### OIDC discovery

```http
GET /auth/openid/config?issuer=<issuer-url>
Authorization: Bearer <access-token>
```

The Audiobookshelf server fetches provider discovery metadata and returns the issuer,
authorization, token, userinfo, logout, and JWKS endpoints plus supported signing algorithms. It
requires admin/root access.

ShelfDroid should call the Audiobookshelf endpoint rather than calling the identity provider
directly. The Retrofit path must remain relative, without a leading slash, so servers installed
under a subpath continue to work.

Reference:

- `server/Auth.js:450-472`
- `client/pages/config/authentication.vue:206-252`

### Runtime application behavior

Changing which strategies are enabled is applied immediately. Editing configuration for an
already active OIDC strategy does not rebuild that strategy, which is why the reference UI warns
that a server restart is required.

The endpoint may accept an enabled OpenID method with incomplete settings and still return
success; OIDC strategy registration then fails. ShelfDroid must validate required OIDC fields
before allowing OpenID to be enabled or Password sign-in to be disabled.

## ShelfDroid findings

### Existing building blocks

ShelfDroid already has most of the surrounding architecture needed for this feature:

- `UserPrefs.isAdmin` is populated for both server `admin` and `root` users.
- The `Misc` screen hides the complete server section unless `isAdmin` is true.
- Dedicated Email and Apprise screens establish the repository, saved/draft state, ViewModel,
  loading/error state, snackbar, validation, and preview patterns for admin server settings.
- `ApiService` already carries bearer credentials through the shared authenticated client.
- Login discovery already reads `authLoginCustomMessage`, active login methods, OpenID button
  text, and auto-launch state from `GET /status`.
- `LoginScreen` already renders the custom HTML message through `AnnotatedString.fromHtml`.
- ShelfDroid's OpenID login already uses `audiobookshelf://oauth`, which must remain in the server's
  allowed mobile redirect URI list.

Reference:

- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/login/LoginMapper.kt:10-23`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:55-171`
- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/emailmanagement/EmailManagementRepository.kt:12-126`
- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/emailmanagement/EmailManagementUiState.kt:6-42`
- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/login/LoginRepository.kt:52-83`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/login/LoginScreen.kt:352-400`
- `app/src/main/java/dev/halim/shelfdroid/auth/OpenIdCallbackActivity.kt:29-39`

### Gaps

- `ApiService` has no `GET/PATCH /api/auth-settings` or OIDC discovery methods.
- There are no authentication-settings request/response models.
- There is no dedicated Authentication navigation key, destination, repository, ViewModel, or
  screen.
- Existing admin navigation hides links but the registered destinations themselves are not
  permission-gated. The Authentication screen must not repeat that weakness because its GET
  response contains a raw secret.
- The broad Server settings screen loads via `/api/authorize` and saves via `/api/settings`. It
  must not be extended for this feature because the server exposes authentication through a
  separate security-sensitive contract.

Reference:

- `core-network/src/main/java/dev/halim/core/network/ApiService.kt:90-481`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/Navigation.kt:116-427`
- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/serversettings/ServerSettingsRepository.kt:9-120`

## Proposed user experience

### Entry and access

Add **Authentication** to the `Server` group in `Misc`, near Users and Server settings. Reuse
`UserPrefs.isAdmin` so both admin and root accounts see it.

If a non-admin destination load is attempted, render an access-denied state and immediately avoid
all Authentication API calls. If the server returns `403`, replace the form with the same
access-denied state and offer Back. Do not show cached or partially loaded values.

### Screen layout

Use one scrollable screen with these sections:

1. **Login message**: enable switch, multiline HTML source editor, and rendered preview.
2. **Login methods**: Password sign-in and OpenID login switches, with an inline error if both are
   disabled.
3. **OpenID provider**: issuer and Auto-populate, endpoint fields, client ID, secret replacement,
   signing algorithm, and callback details.
4. **OpenID user mapping**: existing-user match strategy, auto-launch, auto-register, group claim,
   and advanced permissions claim with sample JSON.
5. **Save area**: dirty-state indication, restart warning, Save, and Reset.

Do not add a rich-text dependency in the first implementation. Editing the HTML source with a live
preview preserves existing server content losslessly and uses ShelfDroid's current login renderer.
A future WYSIWYG editor can replace the source field without changing the network or data model.

### Secret handling

The client-secret field should show only **Configured** or **Not configured** plus actions to
replace or explicitly clear it.

- Never place the raw loaded secret in `AuthenticationSettingsForm`.
- Never store it in DataStore, Room, `SavedStateHandle`, navigation arguments, previews, logs,
  analytics, or test fixtures.
- Keep replacement text only in ViewModel memory.
- Mask replacement input and provide a temporary reveal control.
- Omit `authOpenIDClientSecret` from PATCH when untouched.
- Send the entered replacement only after explicit save.
- Send empty string only after an explicit, confirmation-gated Clear action.
- Never send a visual mask such as `********`; the server would store it literally.

### Save and lockout protection

Before Save:

- Require at least one login method.
- If OpenID is enabled, validate every reference-client-required OIDC field.
- Validate every mobile redirect URI and the wildcard exclusivity rule.
- Restrict existing-user matching to none, email, or username.
- Preserve the current signing algorithm unless discovery provides a replacement selection.
- Preserve the current callback subfolder; only allow empty string or the server-provided base
  path option.
- Require an explicit warning confirmation when Password sign-in is being disabled.
- Block disabling Password sign-in unless OpenID configuration is valid.
- Warn if `audiobookshelf://oauth` is removed because ShelfDroid OpenID login will stop working.

After Save:

- Treat `updated: false` with a non-empty patch as a rejected/skipped update, not success.
- Re-fetch `/api/auth-settings` after `updated: true` and rebuild the saved snapshot.
- Clear the secret replacement immediately after the request completes.
- Show a persistent success message that OIDC configuration changes require an Audiobookshelf
  server restart.
- Do not alter the current ShelfDroid session or force login discovery after saving.

## Proposed architecture

### Network layer

Add a dedicated `authenticationsettings` package under both network request and response packages.

Proposed types:

- `AuthenticationSettingsResponse`
- `UpdateAuthenticationSettingsRequest`
- `UpdateAuthenticationSettingsResponse`
- `OpenIdConfigurationResponse`

Add `ApiService` methods:

```kotlin
@GET("api/auth-settings")
suspend fun authenticationSettings(): Result<AuthenticationSettingsResponse>

@PATCH("api/auth-settings")
suspend fun updateAuthenticationSettings(
  @Body request: UpdateAuthenticationSettingsRequest
): Result<UpdateAuthenticationSettingsResponse>

@GET("auth/openid/config")
suspend fun discoverOpenIdConfiguration(
  @Query("issuer") issuer: String
): Result<OpenIdConfigurationResponse>
```

All update request fields should default to null so unchanged values are omitted by the existing
serializer. The mapper must use empty string for an explicit clear.

### Data layer

Add a screen-focused package at
`core-data/.../screen/authenticationsettings/` containing:

- `AuthenticationSettingsRepository`
- `AuthenticationSettingsMapper`
- `AuthenticationSettingsUiState`
- `AuthenticationSettingsForm`
- Validation and operation/result types

The repository should inject `ApiService` and `PrefsRepository`.

Repository responsibilities:

- Read current `UserPrefs` before every load or mutation.
- Return `AccessDenied` without an API call when `isAdmin` is false.
- Map `403` to `AccessDenied` and other failures to operation-specific errors.
- Map the raw secret to `clientSecretConfigured` and discard the value.
- Keep saved and draft form snapshots for dirty checking.
- Build a changed-fields-only PATCH request.
- Distinguish unchanged, replace-secret, and clear-secret intent.
- Run issuer discovery and merge only provider-owned fields, preserving user-entered client data.
- Reload canonical Authentication settings after a successful update.

Use a specific UI state sealed type or explicit access state rather than encoding denial as a
generic error string. A suggested top-level state is `Loading`, `AccessDenied`, `Ready`, or
`Failure` plus a separate mutation state.

### UI layer

Add:

- `AuthenticationSettingsViewModel`
- `AuthenticationSettingsScreen`
- Small section composables where they improve preview and test coverage
- Strings for field labels, descriptions, validation, confirmations, and results

The ViewModel should expose immutable state, own all transient secret-replacement text, validate
before delegating to the repository, and serialize save/discovery operations so results cannot
race with edits.

Follow the existing Email and Apprise patterns for state collection, snackbar handling, previews,
and saved/draft updates. Do not put network DTOs directly in Compose state.

### Navigation

Add `AuthenticationSettings` to `ShelfNavKey`, a destination entry in `Navigation.kt`, and an
`onAuthenticationSettingsClicked` callback through `HomeScreen` and `MiscScreen`.

The `Misc` entry remains inside the existing `if (isAdmin)` section. Destination-level access is
still required because route visibility is presentation, not authorization.

### Domain documentation

`CONTEXT.md` defines **Authentication settings** and distinguishes them from Login methods, Login
discovery, User management, Server settings, and Local app preferences.

ADR 0013 records that Authentication settings use a dedicated admin/root screen under `Misc`, use
`/api/auth-settings`, do not reuse Server settings, and reduce the loaded client secret to
configured/not-configured. This follows the precedents in ADR 0005 for Email settings and ADR 0006
for Apprise notification settings.

## Implementation sequence

1. **Contract tests and models**: add redacted fixtures for GET, partial PATCH, clear semantics,
   discovery, `updated: false`, and unknown fields.
2. **Network endpoints**: add dedicated ApiService methods and request/response models; verify
   subpath-safe URLs.
3. **State and mapper**: implement saved/draft state, field validation, secret intent, and
   changed-field request generation.
4. **Repository authorization**: add local admin/root checks before every API call and map server
   `403` to access denial.
5. **Repository operations**: implement load, discovery, save, canonical reload, and operation
   errors.
6. **Compose screen**: build accessible sections, HTML preview, redirect URI editor, secret
   replacement flow, warnings, confirmations, loading/error states, and previews.
7. **Navigation**: add the admin-only `Misc` entry, key, destination, and defensive destination
   access state.
8. **Login integration verification**: confirm saved custom messages, button text, active methods,
   auto-launch, and `audiobookshelf://oauth` behavior remain compatible with current login
   discovery.
9. **Manual server verification**: test against admin, root, and non-admin accounts on an
    Audiobookshelf server installed both at root and under a subpath.

## Test plan

### Network serialization tests

- Decode a complete authentication-settings response without logging or asserting a real secret.
- Omit unchanged null request fields under the shared `explicitNulls = false` configuration.
- Serialize empty string when clearing a normal string field or client secret.
- Preserve empty string for `authOpenIDSubfolderForRedirectURLs`.
- Serialize only changed login methods and fields.
- Keep the OIDC discovery route relative for subpath servers.

### Mapper and validation tests

- Map raw client-secret presence to configured/not-configured and discard the value.
- Require at least one login method.
- Reject enabling OpenID with missing required configuration.
- Reject disabling Password sign-in with invalid OpenID configuration.
- Accept valid redirect URIs and reject invalid entries.
- Reject a wildcard combined with another redirect URI.
- Warn when `audiobookshelf://oauth` is removed.
- Restrict match-existing values to none, email, or username.
- Preserve current signing algorithm until discovery succeeds.
- Map disabled or blank custom message to an explicit clear.
- Preserve complex existing custom-message HTML until the admin edits it.

### Repository tests

- Non-admin load makes zero Authentication API calls and returns `AccessDenied`.
- Non-admin discovery and save make zero API calls.
- Server `403` replaces ready state with `AccessDenied` and drops form data.
- Successful load creates identical saved and draft snapshots.
- No changes produce no PATCH request.
- A normal edit sends only changed fields.
- Untouched client secret is omitted.
- Secret replacement is sent once and then removed from memory.
- Explicit secret clearing sends empty string only after confirmation.
- `updated: false` for a non-empty patch is surfaced as failure.
- Successful save reloads canonical settings.
- Discovery failure preserves the current draft.
- Discovery success updates provider endpoints and algorithm options without overwriting client ID,
  secret intent, mapping rules, or redirect URIs.

### ViewModel and UI tests

- Loading, ready, failure, access-denied, saving, and discovery states render correctly.
- The `Misc` Authentication entry is visible for `UserPrefs.isAdmin` and absent otherwise.
- Save is disabled when clean, invalid, or already saving.
- Reset restores the saved snapshot and clears transient secret input.
- Password sign-in disablement requires confirmation.
- Clear secret requires confirmation.
- Every field and toggle has a label and TalkBack description.
- Client-secret text is masked and absent from previews and semantics snapshots.
- The restart warning appears after OIDC changes save successfully.
- Back navigation clears transient secret input.

### Manual verification matrix

| Scenario | Expected result |
| --- | --- |
| Admin account | Entry visible; load, discovery, and save work. |
| Root account | Same access as admin. |
| User or guest | Entry hidden; forced destination load performs no GET and shows access denied. |
| Role revoked while screen is open | Next request receives `403`, clears displayed settings, and shows access denied. |
| Root-installed server | All three endpoints resolve correctly. |
| Subpath-installed server | `/api/auth-settings` and `/auth/openid/config` retain the server base path. |
| Password only | Save preserves local login and hides/disables OpenID details appropriately. |
| OpenID only | Save requires valid OIDC data and explicit local-login-disable confirmation. |
| Both methods | Login discovery continues to show both methods. |
| Secret untouched | PATCH omits the secret. |
| Secret replaced | Replacement is sent once, masked, and discarded after completion. |
| Redirect URI removed | Warning identifies that ShelfDroid OpenID login will no longer work. |

## Acceptance criteria

- Only admin and root users can discover or open Authentication management.
- A locally known non-admin user triggers no Authentication settings API request.
- Server `403` is handled as access denial, not a generic retry loop.
- The screen reads and updates `/api/auth-settings` without reusing `/api/settings`.
- Save uses a changed-fields-only PATCH and preserves all untouched server values.
- The OIDC client secret is never persisted, logged, previewed, or redisplayed.
- At least one login method is always required.
- Password sign-in cannot be disabled while OpenID configuration is invalid.
- OIDC discovery works for root and subpath Audiobookshelf installations.
- The app warns before removing `audiobookshelf://oauth`.
- A successful OIDC update clearly states that the Audiobookshelf server must be restarted.
- Existing Login discovery continues to honor the saved login methods, custom message, OpenID
  button text, and auto-launch setting.
- Unit tests cover authorization, mapping, validation, request serialization, secret intent,
  discovery, and update outcomes.

## Risks and explicit tradeoffs

- **Raw secret in GET response**: unavoidable in the upstream contract. Mitigation is immediate
  reduction to configured/not-configured state and no local persistence.
- **Server silently skips invalid fields**: client validation and `updated: false` handling are
  required; HTTP `200` alone is not proof of a successful change.
- **Authentication lockout**: confirmation and OIDC completeness checks reduce risk but cannot
  prove the external identity provider is functioning. Retaining Password sign-in is the safest
  default.
- **OIDC restart requirement**: ShelfDroid can report it but cannot guarantee the server was
  restarted.
- **HTML editor scope**: source plus preview is less polished than the web rich-text editor but
  preserves data without adding a large editor dependency.

## Primary source index

Audiobookshelf:

- `client/pages/config/authentication.vue:1-370`
- `client/components/app/ConfigSideNav.vue:44-118`
- `server/controllers/MiscController.js:615-733`
- `server/routers/ApiRouter.js:353-354`
- `server/Auth.js:450-472`
- `server/objects/settings/ServerSettings.js:64-85,263-319`
- `server/models/User.js:538-543`
- `server/auth/OidcAuthStrategy.js:62-95`

ShelfDroid:

- `CONTEXT.md`
- `docs/adr/0004-session-recovery-forced-relogin-and-full-logout.md`
- `docs/adr/0005-dedicated-admin-email-management-screen.md`
- `docs/adr/0006-dedicated-admin-apprise-notification-settings-screen.md`
- `core-network/src/main/java/dev/halim/core/network/ApiService.kt:90-481`
- `core-network/src/main/java/dev/halim/core/network/di/NetworkModule.kt:25-35`
- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/login/LoginRepository.kt:52-83`
- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/emailmanagement/EmailManagementRepository.kt:12-126`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:55-171`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/navigation/Navigation.kt:116-427`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/login/LoginScreen.kt:352-400`
- `app/src/main/java/dev/halim/shelfdroid/auth/OpenIdCallbackActivity.kt:29-39`
