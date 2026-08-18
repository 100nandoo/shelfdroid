# 01 — Admin-only Authentication settings overview

**What to build:** Add a narrow, read-only path through navigation, reusable admin access control,
Authentication settings loading, state mapping, and UI so that admin and root Users can inspect
the Audiobookshelf server's current Login methods and OpenID configuration without exposing its
raw client secret. Regular and guest Users must not discover or load the feature.

**Blocked by:** None — can start immediately.

**Status:** ready-for-human

- [x] Authentication appears in the existing Server section for admin and root Users only.
- [x] A reusable admin-destination guard protects the Authentication destination without migrating
      unrelated existing admin destinations.
- [x] A locally known regular or guest User cannot open the destination and causes zero
      Authentication settings HTTP requests.
- [x] The screen loads the dedicated Authentication settings endpoint through the authenticated
      client for both root and subpath server installations.
- [x] A successful response renders the custom-message status, active Login methods, and a
      read-only summary of OpenID provider, callback, and User-mapping configuration.
- [x] The raw OpenID client secret is reduced immediately to configured/not-configured and is never
      rendered, persisted, logged, placed in navigation state, or included in previews.
- [x] Loading and non-authorization failures render distinct, retryable states without showing
      partial settings.
- [x] A server `403` renders access denied, discards displayable settings, and offers Back rather
      than retrying indefinitely.
- [x] Repository tests cover admin, root, non-admin zero-request behavior, successful mapping,
      server `403`, ordinary failure, secret reduction, and subpath URL resolution.
- [x] UI coverage verifies admin-only entry visibility plus loading, ready, failure, and
      access-denied states without asserting private layout structure.

## Verification

Automated checks passed:

- `./gradlew :core-network:compileDebugKotlin :core-data:compileDebugKotlin :core-ui:compileDebugKotlin`
- `./gradlew :core-data:testDebugUnitTest --tests 'dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsRepositoryTest' :core-ui:testDebugUnitTest --tests 'dev.halim.shelfdroid.core.ui.screen.home.MiscScreenTest'`
- `./gradlew :core-ui:compileDebugAndroidTestKotlin` (includes the four-state
  `AuthenticationSettingsContentTest` coverage)
- `git diff --check`

The broader `./gradlew :core-data:check :core-ui:check` check was also run: module tests and
compilation passed, but the aggregate check failed on pre-existing lint errors outside this ticket.
The instrumentation suite could not run in this environment because
`./gradlew :core-ui:connectedDebugAndroidTest` reported `No connected devices!`; the test source
does compile successfully and asserts only user-visible state text and actions.
Manual verification remains for an admin and root account against Audiobookshelf servers installed
at the root and under a URL subpath, plus visual TalkBack/Back behavior and role revocation while
the screen is open. The full module lint check currently reports pre-existing errors in
`core-data/src/main/.../LoginRepository.kt`,
`core-data/src/main/.../OpenIdCallbackCoordinator.kt`,
`core-ui/src/main/.../EditEpisodeMatchTab.kt`, and
`core-ui/src/main/.../PreviewWrapper.kt`; no lint findings were reported for the ticket files.
