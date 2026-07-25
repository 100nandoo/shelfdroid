# Authentication Management Findings

## Scope

This note captures the current Audiobookshelf authentication-management UI and API behavior, compares it with ShelfDroid's current implementation, and records the main implementation constraints for adding authentication management to ShelfDroid.

The live admin route confirmed in Chrome DevTools MCP was:

- `http://192.168.50.150:13378/audiobookshelf/config/authentication/`

## Audiobookshelf UI Findings

The upstream admin UI is a dedicated authentication settings page, not a subsection of the generic server settings page.

- Route and page source: `audiobookshelf/client/pages/config/authentication.vue:1-388`
- Admin access is enforced in `asyncData`; non-admin users are redirected away.
- The page loads data from `GET /api/auth-settings`.
- The page saves data with `PATCH /api/auth-settings`.

The page is organized around three main areas:

- Custom login message
  - Toggle controls whether a rich-text custom message is shown on the login screen.
- Local password authentication
  - Single checkbox toggles the `local` auth method.
- OpenID Connect authentication
  - Single checkbox toggles the `openid` auth method.
  - Expanded form includes issuer URL, authorization URL, token URL, userinfo URL, JWKS URL, logout URL, client ID, client secret, token signing algorithm, mobile redirect URIs, redirect subfolder, button text, existing-user matching mode, auto-launch, auto-register, group claim, and advanced permission claim.

Other UI behavior on the admin page:

- "Auto-populate" calls `/auth/openid/config?issuer=...` and fills issuer-derived OpenID endpoints.
- Client-side validation blocks save when:
  - no auth method is enabled,
  - required OpenID fields are missing,
  - mobile redirect URIs are invalid,
  - claim names are invalid.
- The page shows a warning that OIDC changes may require a restart.

## Audiobookshelf API Findings

### Admin auth-settings endpoints

The auth-management page is backed by a dedicated API surface, separate from `/api/settings`.

- Route wiring: `audiobookshelf/server/routers/ApiRouter.js:353-354`
- `GET /api/auth-settings` implementation: `audiobookshelf/server/controllers/MiscController.js:621-627`
- `PATCH /api/auth-settings` implementation: `audiobookshelf/server/controllers/MiscController.js:636-733`

Behavior:

- `GET /api/auth-settings` is admin-only and returns `Database.serverSettings.authenticationSettings`.
- `PATCH /api/auth-settings` is admin-only and accepts a partial update object.
- The patch path validates:
  - `authActiveAuthMethods` against supported auth methods,
  - `authOpenIDMobileRedirectURIs`,
  - booleans for `authOpenIDAutoLaunch` and `authOpenIDAutoRegister`,
  - string-or-null values for the remaining fields.
- After a successful update, the server persists settings and enables/disables auth strategies via `useAuthStrategy()` and `unuseAuthStrategy()`.

### Auth settings payload shape

The auth page reads a dedicated auth-settings projection rather than the full server settings payload.

- Projection source: `audiobookshelf/server/objects/settings/ServerSettings.js:285-320`

`authenticationSettings` includes:

- `authLoginCustomMessage`
- `authActiveAuthMethods`
- OpenID issuer, endpoints, credentials, signing algorithm, button text
- auto-launch and auto-register flags
- matching mode
- mobile redirect URIs
- group claim and advanced permissions claim
- redirect subfolder
- sample permissions text

`authFormData` is a smaller login-facing payload:

- `authLoginCustomMessage`
- `authOpenIDButtonText`
- `authOpenIDAutoLaunch`

### OpenID-specific routes

- Auth route setup: `audiobookshelf/server/Auth.js:318-512`
- Auto-populate endpoint: `audiobookshelf/server/Auth.js:467-485`

Relevant routes:

- `POST /login`
- `POST /auth/refresh`
- `GET /auth/openid`
- `GET /auth/openid/mobile-redirect`
- `GET /auth/openid/callback`
- `GET /auth/openid/config`
- `POST /logout`

OpenID details that matter for mobile clients:

- The mobile flow uses browser redirect plus callback handling.
- `/auth/openid/callback` accepts `code_verifier` for PKCE.
- `/logout` may return an upstream IdP logout redirect URL when the active auth method is OpenID.

## Login-Surface Behavior In Audiobookshelf

### Server status payload drives login UI

Audiobookshelf exposes active auth methods and login-facing auth metadata through `/status`.

- `/status` payload: `audiobookshelf/server/Server.js:349-358`

The payload includes:

- `authMethods`
- `authFormData`
- `serverVersion`
- `language`
- `isInit`

### Web login behavior

- Login page source: `audiobookshelf/client/pages/login.vue:80-320`

Behavior:

- `GET /status` decides whether local login fields and/or OpenID login button should be shown.
- If `authOpenIDAutoLaunch` is set, the web client automatically redirects into the OpenID flow unless `?autoLaunch=0` disables it.
- The login page shows `authLoginCustomMessage`.
- The OpenID button label comes from `authOpenIDButtonText`.

### Upstream mobile app behavior

- Mobile connection form: `audiobookshelf-app/components/connection/ServerConnectForm.vue:1-981`

Behavior:

- The mobile app first calls `/status`.
- It uses `authMethods` to decide whether to show username/password, OpenID button, or both.
- It uses `authFormData.authOpenIDButtonText` for the button label.
- It auto-starts OIDC when `authFormData.authOpenIDAutoLaunch` is true.
- The OpenID flow uses browser-based login plus PKCE and app callback handling.
- The app later uses `/api/authorize` to validate the returned token and finish login state.

## ShelfDroid Current State

### What exists already

- Login repository: `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/login/LoginRepository.kt:16-79`
- Login screen: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/login/LoginScreen.kt:91-200`
- API surface: `core-network/src/main/java/dev/halim/core/network/ApiService.kt:81-417`
- Admin misc menu: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/MiscScreen.kt:53-161`
- Server settings repository: `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/serversettings/ServerSettingsRepository.kt:9-120`
- Server settings screen: `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/serversettings/ServerSettingsScreen.kt:53-96`
- Current server-settings network model: `core-network/src/main/java/dev/halim/core/network/response/Login.kt:83-114`

Current behavior:

- ShelfDroid login is local username/password only.
- `LoginScreen` has server, username, password, and login button; there is no `/status`-driven auth-method selection.
- `LoginRepository.login()` only calls `POST /login`.
- `ApiService` already exposes `login`, `authorize`, `refresh`, `logout`, `closeSession`, and related auth/session endpoints.
- `ApiService` does not expose:
  - `GET /api/auth-settings`
  - `PATCH /api/auth-settings`
  - `GET /auth/openid/config`
  - `GET /status`
  - any browser/OIDC callback handling surface
- `ServerSettingsRepository` currently maps `/api/authorize` plus `/api/settings`, not `/api/auth-settings`.
- ShelfDroid's `ServerSettings` response model does not include the dedicated auth-management fields used by Audiobookshelf.

### Current navigation shape

ShelfDroid already has a precedent for dedicated admin-only screens under `Misc`.

- Admin menu currently exposes separate entries for:
  - server settings
  - email management
  - API keys
  - users
  - logs
  - backups
  - open sessions
- Dedicated email-management ADR: `docs/adr/0005-dedicated-admin-email-management-screen.md:1`

That precedent matters because Audiobookshelf auth management is also a separate API surface with a large, specialized UI and login-critical behavior.

## Main Implementation Constraint

The biggest product risk is configuration lockout.

If ShelfDroid adds an admin auth-management screen before ShelfDroid can also consume the resulting login configuration, an admin could:

- disable local auth,
- enable OIDC auto-launch,
- change the OpenID button text and login message,

and then ShelfDroid would no longer match the server's expected login flow.

Today ShelfDroid has no `/status`-driven login rendering and no OIDC browser callback flow, so it cannot safely mirror the full upstream auth-management feature yet.

## Recommended ShelfDroid Shape

Implement this as a dedicated admin screen under the `Misc` admin cluster, not as another section inside `ServerSettingsScreen`.

Reasons:

- Upstream uses a dedicated route and a dedicated `/api/auth-settings` API surface.
- The UI is large and identity-specific.
- The save behavior is distinct from generic `/api/settings`.
- Misconfiguration can lock users out of the app, which deserves isolation and stronger warnings.
- ShelfDroid already uses this screen-splitting approach for email management.

## Recommended Rollout Order

### Phase 1: Login compatibility

Add the minimum client-side auth-consumption features first:

- `GET /status` model and request
- login UI that reacts to `authMethods`
- display of `authLoginCustomMessage`
- OpenID button label support
- browser-based OIDC + PKCE flow
- callback handling and token finalization through `/auth/openid/callback` and `/api/authorize`

Without this phase, auth management can put ShelfDroid into a state it cannot log into.

### Phase 2: Admin auth-management API layer

Add network/data support for:

- `GET /api/auth-settings`
- `PATCH /api/auth-settings`
- `GET /auth/openid/config?issuer=...`

Add dedicated request/response models instead of extending the existing generic `ServerSettings` model, because the upstream server already treats auth settings as a separate payload.

### Phase 3: Admin auth-management UI

Mirror the upstream page structure:

- custom login message section
- local auth toggle
- OpenID enable/toggle section
- OpenID details form
- auto-populate issuer helper
- save button with change detection
- warning copy before changes that may affect future logins

### Phase 4: Guardrails

Before allowing save, add app-side warnings for lockout scenarios, especially when:

- local auth is being disabled,
- OpenID is being enabled without verified ShelfDroid OIDC support,
- mobile redirect URIs do not include the ShelfDroid callback URI.

## Bottom Line

ShelfDroid can implement Audiobookshelf authentication management, but it should not start with only the admin screen.

The upstream feature is coupled to:

- `/status`
- `authFormData`
- dynamic login-method rendering
- browser-based OpenID login

So the safe implementation order is:

1. make ShelfDroid consume the server's auth configuration at login,
2. add the dedicated admin auth-management screen and API layer,
3. add lockout warnings around destructive auth changes.
