# OpenID Login And Callback Handling Plan

## Scope

This plan covers adding browser-based **OpenID login** to ShelfDroid's existing login surface and
completing sign-in when Android returns control to the app.

This plan assumes the current workspace baseline already exists:

- **Login discovery** through `GET /status`
- login-surface storage of server-advertised **Login methods**
- disabled **OpenID login** button scaffolding in the login UI

In scope:

- starting **OpenID login** from the current login screen
- PKCE and state generation and persistence
- browser launch
- Android callback intent handling
- code exchange and login finalization
- reuse of the existing post-login success path
- tests for the new flow

Out of scope for the first slice:

- admin `/api/auth-settings` screens
- `authOpenIDAutoLaunch`
- upstream IdP logout handling
- replacing the existing **Local login** flow

## Current Baseline

ShelfDroid already has the first half of the login-compatibility work.

- `ApiService.status(...)` exists and already uses the anonymous-request marker:
  `core-network/src/main/java/dev/halim/core/network/ApiService.kt`
- anonymous requests already skip bearer-token injection and forced re-login:
  `core-network/src/main/java/dev/halim/core/network/client/AnonymousRequest.kt`
  `core-network/src/main/java/dev/halim/core/network/client/TokenAuthenticator.kt`
  `core-network/src/test/java/dev/halim/core/network/client/AnonymousRequestTest.kt`
- `LoginRepository.discoverLoginMethods(...)` already reads `/status` and stores:
  - available **Login methods**
  - custom login message
  - OpenID button text
  - OpenID auto-launch flag
- `LoginViewModel` already debounces host changes and runs **Login discovery**:
  `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/login/LoginViewModel.kt`
- `LoginScreen` already renders disabled OpenID buttons for mixed and OpenID-only states:
  `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/login/LoginScreen.kt`

What is still missing:

- no manifest callback intent filter
- no browser launch service
- no PKCE/state persistence
- no `/auth/openid` start flow
- no `/auth/openid/callback` exchange flow
- no success-path reuse for OpenID

## Platform Guidance

Two Android platform details should drive the design.

- Android deep-link handling should process incoming `ACTION_VIEW` intent data from both
  `onCreate()` and `onNewIntent()`.
  Source: Android Developers app-link handling guidance retrieved through Context7.
- `androidx.browser.customtabs.CustomTabsIntent` provides a dedicated browser-launch API through
  `launchUrl(context, uri)`.
  Source: AndroidX browser reference retrieved through Context7.

## Key Decisions

### 1. Treat **OpenID login** as its own login path

Do not expand `LoginRepository.login()` into a dual-purpose credentials-plus-browser coordinator.

Reason:

- `LoginRepository.login()` is currently a compact **Local login** submit path.
- OpenID startup and callback exchange add browser, PKCE, redirect, and callback concerns that are
  easier to test when isolated.

### 2. Use a dedicated callback activity in the `app` module

Add a small `OpenIdCallbackActivity` under `app/src/main/java/...`.

Reason:

- the manifest already lives in the `app` module
- callback activities are application entry points, not reusable Compose screens
- a dedicated activity avoids mixing auth callback parsing into `MainActivity`
- it also avoids login-screen flicker during cold-start callback processing

### 3. Derive the callback URI from `applicationId`

Use a custom URI scheme derived from the actual app id, for example:

- release: `dev.halim.shelfdroid://oauth`
- debug: `dev.halim.shelfdroid.debug://oauth`

Reason:

- `app/build.gradle.kts` already uses `applicationIdSuffix = ".debug"`
- deriving the scheme from `applicationId` avoids release/debug collisions on the same device
- the server's mobile redirect URI list can explicitly include both values

### 4. Persist PKCE context across process death

Store the pending OpenID context outside Compose state and outside in-memory singletons.

Minimum stored fields:

- normalized server
- code verifier
- state
- created-at timestamp
- optional cookies captured from the `/auth/openid` start response

Reason:

- the browser round-trip can outlive the current activity or process
- callback validation must still work after app recreation

### 5. Reuse `LoginSuccessHandler` for final success

Do not build a second post-login success path.

`LoginSuccessHandler.onLoginSuccess(server, response)` already:

- updates base URL
- persists tokens and user prefs
- populates progress and bookmarks

That should remain the single success seam for both **Local login** and **OpenID login**.

### 6. Ship manual OpenID button support before auto-launch

The first rollout should make the OpenID button work, but should not automatically launch the
browser even if `authOpenIDAutoLaunch` is present.

Reason:

- callback and browser handling are the risky parts
- auto-launch adds UX and lifecycle complexity on top of that
- the current workspace already stores the auto-launch flag, so a follow-up slice can consume it

## Proposed Touchpoints

### App entrypoint

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- new `app/src/main/java/dev/halim/shelfdroid/auth/OpenIdCallbackActivity.kt`

### Login UI and state

- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/login/LoginScreen.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/login/LoginViewModel.kt`

### Login domain and orchestration

- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/login/LoginRepository.kt`
- `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/login/LoginSuccessHandler.kt`
- new `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/login/OpenIdLoginCoordinator.kt`
- new `core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/login/PendingOpenIdLoginStore.kt`

### Network layer

- `core-network/src/main/java/dev/halim/core/network/ApiService.kt`
- `core-network/src/main/java/dev/halim/core/network/client/AnonymousRequest.kt`
- `core-network/src/main/java/dev/halim/core/network/client/TokenAuthenticator.kt`
- possibly a new small `OpenIdStartService` or raw OkHttp helper for the initial redirect request

### Tests

- `core-data/src/test/.../LoginRepositoryTest.kt`
- `core-ui/src/test/.../LoginViewModelStateTest.kt`
- `core-network/src/test/.../AnonymousRequestTest.kt`
- new tests for callback parsing and OpenID flow coordination

## Implementation Plan

### Phase 1. Enable the existing OpenID button scaffold

Goal:

- make the current disabled OpenID UI emit a real login event

Work:

- add `LoginEvent.OpenIdLoginButtonPressed`
- handle it in `LoginViewModel`
- keep current mixed-login rendering behavior:
  - **Local login** fields remain the default visible form
  - OpenID appears as an alternate action
- keep current OpenID-only rendering behavior:
  - the OpenID button becomes enabled and primary

Acceptance:

- mixed-method servers show working **Local login** plus a tappable **OpenID login** action
- OpenID-only servers show a working **OpenID login** button instead of a disabled placeholder

### Phase 2. Add pending OpenID context storage

Goal:

- persist enough state to safely survive the browser round-trip

Work:

- add a small pending-login store with:
  - normalized server
  - state
  - code verifier
  - timestamp
  - optional start-response cookies
- add clear-on-success and clear-on-failure paths
- reject stale callback contexts by age

Implementation note:

- keep this store separate from long-lived `UserPrefs`
- this is transient auth state, not user identity state

Acceptance:

- the app can validate callback `state` even after process recreation
- stale or missing callback context produces a clean user-visible failure

### Phase 3. Add OpenID start flow

Goal:

- convert a button press into a browser-based auth launch

Work:

- add a dedicated OpenID start coordinator, not more logic inside `LoginRepository.login()`
- generate PKCE and state
- construct the `/auth/openid` request against the normalized server with:
  - `code_challenge`
  - `code_challenge_method=S256`
  - `redirect_uri`
  - `client_id`
  - `response_type=code`
  - `state`
- capture the initial redirect response and any `Set-Cookie` headers
- persist the pending context
- launch the browser

Important seam:

- prefer a small dedicated OkHttp path for the initial `/auth/openid` request, with redirects
  disabled, because the shared Retrofit flow is optimized for parsed JSON responses, not for
  reading the first 302 `Location` and cookies

Browser choice:

- prefer `CustomTabsIntent.launchUrl(...)`
- fall back to `Intent.ACTION_VIEW` if a Custom Tabs provider is unavailable

Acceptance:

- pressing **OpenID login** opens the provider/browser flow for the typed server
- the app preserves the PKCE/state context before launching the browser

### Phase 4. Add Android callback handling

Goal:

- receive the browser redirect and safely validate it

Work:

- add a manifest `intent-filter` for `ACTION_VIEW`
- match the chosen callback scheme/host
- implement a dedicated `OpenIdCallbackActivity`
- route all incoming intents through one handler method from both `onCreate()` and `onNewIntent()`
- validate:
  - callback scheme/host
  - presence of `code` or provider `error`
  - returned `state` against stored context

Error handling:

- clear the pending OpenID context on failure
- show a user-visible failure
- return the user to the existing login surface

Acceptance:

- the callback URI re-enters ShelfDroid reliably from browser auth
- invalid or replayed state fails closed

### Phase 5. Exchange the callback code and finalize login

Goal:

- turn the callback code into a normal ShelfDroid logged-in session

Work:

- call `/auth/openid/callback` with:
  - `state`
  - `code`
  - `code_verifier`
- keep that request anonymous so it never triggers token refresh or forced re-login
- normalize the resulting auth payload into the existing `LoginResponse` success path

Preferred success seam:

1. exchange the code
2. obtain or normalize a `LoginResponse`
3. call `loginSuccessHandler.onLoginSuccess(normalizedServer, response)`

Normalization rule:

- if `/auth/openid/callback` reliably returns the same effective payload shape as `/login`, pass it
  directly to `LoginSuccessHandler`
- if server-version differences make that payload incomplete, add a follow-up canonicalization step
  using `/api/authorize` before invoking `LoginSuccessHandler`

This decision should be made during implementation based on live server behavior, but the plan must
preserve the single-success-seam rule either way.

Acceptance:

- successful OpenID callback produces the same persisted login state as **Local login**
- the app lands in its normal logged-in navigation state after success

### Phase 6. Re-login and lifecycle integration

Goal:

- make the new path behave correctly during **Forced re-login** and activity recreation

Work:

- preserve current **Forced re-login** messaging
- allow **OpenID login** during **Forced re-login** when the server only offers OpenID
- avoid showing stale **Local login** UI after a successful `/status` discovery
- on callback success, launch or resume `MainActivity` cleanly so token-driven navigation takes the
  user to home

Guideline:

- do not special-case OpenID inside `MainActivity` navigation logic if the token flow already
  drives the correct destination after `LoginSuccessHandler`

Acceptance:

- OpenID-only servers remain usable during **Forced re-login**
- the callback flow does not strand the user on a blank or stale screen

## Testing Plan

### Unit tests

- PKCE/state generation and storage
- callback URI parsing
- invalid-state rejection
- expired pending-context rejection
- start-flow request building for normalized server URLs and subpaths
- success-path reuse through `LoginSuccessHandler`

### Existing test suites to extend

- `core-data/.../LoginRepositoryTest.kt`
- `core-ui/.../LoginViewModelStateTest.kt`
- `core-network/.../AnonymousRequestTest.kt`

### Instrumentation tests

- OpenID button becomes enabled for OpenID-capable discovery results
- callback activity handles success and provider-error URIs
- successful callback returns the user to the normal logged-in app state

### Manual verification

- mixed-method server: **Local login** still works, **OpenID login** works
- OpenID-only server: OpenID flow works from first screen
- subpath install: callback flow still uses the correct normalized base URL
- bad callback state: login is rejected and local session state stays unchanged
- process death between browser launch and callback: callback still validates or fails cleanly

## Risks And Mitigations

### Risk: callback payload shape differs from `/login`

Mitigation:

- keep final success behind `LoginSuccessHandler`
- add a canonical `/api/authorize` normalization step if direct callback payloads prove incomplete

### Risk: cookies from the `/auth/openid` start request matter

Mitigation:

- capture and persist `Set-Cookie` headers from the redirect-start response if present
- replay them during callback exchange if the server requires them

### Risk: custom-scheme collisions or debug/release conflicts

Mitigation:

- derive callback scheme from `applicationId`
- document both release and debug redirect URIs in the implementation note and test checklist

### Risk: browser/callback UX is noisy on failure

Mitigation:

- start with a small, explicit failure surface
- if a callback-activity toast is too rough, follow with a one-shot snackbar handoff to the login
  screen in a later cleanup pass

## Recommended Delivery Order

1. Wire the current OpenID buttons to a real event.
2. Add pending PKCE/state storage.
3. Add the OpenID start request and browser launch.
4. Add callback activity and state validation.
5. Add callback code exchange and success finalization through `LoginSuccessHandler`.
6. Add tests and manual verification against:
   - mixed-method server
   - OpenID-only server
   - **Forced re-login**

## Follow-up After This Plan Lands

- implement `authOpenIDAutoLaunch`
- update `docs/plan/authentication-management-findings.md` so it no longer describes ShelfDroid as
  lacking all `/status`-driven login behavior
- then proceed with admin auth-management implementation, because the login-surface lockout risk
  will be materially reduced
