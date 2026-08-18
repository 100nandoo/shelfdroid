# 07 — Harden and verify Authentication settings

**What to build:** Integrate the completed Authentication settings slices into a release-ready
admin experience, close cross-slice authorization, concurrency, and accessibility gaps, and verify
behavior against real Audiobookshelf roles and installation shapes.

**Blocked by:** 04 — Rotate the OpenID client secret securely; 05 — Manage mobile OpenID
callbacks; 06 — Configure OpenID User mapping and registration.

**Status:** ready-for-human

- [x] Load, discovery, and Save operations are serialized so stale results cannot overwrite newer
      admin edits.
- [x] Role revocation during an open screen causes the next server `403` to clear settings and
      transient secret input, remove sensitive UI, and show access denied.
- [x] Every field, switch, action, validation message, warning, and operation result has an
      appropriate visible label and TalkBack semantics.
- [x] Keyboard, focus, scrolling, and error-navigation behavior allow the complete form to be used
      on supported phone and tablet sizes.
- [x] Preview and UI coverage includes loading, ready, dirty, invalid, saving, discovery, failure,
      access-denied, Password-only, OpenID-only, both-methods, and secret-replacement states.
- [x] End-to-end verification confirms saved custom message, Login methods, OpenID button text,
      and auto-launch behavior appear through Login discovery after the server applies changes.
- [ ] Manual verification passes for admin, root, user, guest, and a User whose admin role is
      revoked while the screen is open.
- [ ] Manual verification passes for Audiobookshelf installed at the URL root and under a subpath.
- [ ] Manual verification covers Password-only, OpenID-only, both Login methods, issuer discovery,
      secret replacement, secret clearing, wildcard redirect, and removal of the ShelfDroid
      callback URI.
- [x] The current ShelfDroid session remains active after Save; the feature does not force logout,
      restart ShelfDroid, rerun Login discovery for the current session, or restart the server.
- [x] All affected module tests and project formatting checks pass without weakening existing
      Email settings, Apprise notification settings, Login, session recovery, or navigation tests.
- [x] Screen-capture and recent-app-preview protection remains deferred.

## Automated verification

- `./gradlew :core-data:test :core-ui:test` — passed.
- `./gradlew :core-ui:compileDebugAndroidTestKotlin` — passed.
- `./gradlew :core-ui:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.halim.shelfdroid.core.ui.screen.authenticationsettings.AuthenticationSettingsContentTest` — 7 tests passed on the local Android 16 emulator.
- `git diff --check` — passed.
- `./gradlew :core-ui:lintDebug` remains blocked by three pre-existing errors in unrelated
  `EditEpisodeMatchTab.kt` and `PreviewWrapper.kt`; no lint errors were reported for the changed
  Authentication settings files.

## Manual verification remaining

- Run the Authentication screen against admin, root, regular User, guest, and role-revocation
  accounts; confirm navigation visibility and server-authoritative `403` behavior.
- Verify Audiobookshelf URL-root and URL-subpath installations, Password-only/OpenID-only/both
  Login methods, issuer discovery, secret replacement/clearing, wildcard redirects, and removal
  of `audiobookshelf://oauth`.
- Confirm keyboard/focus/error navigation on supported phone and tablet sizes and confirm the
  current ShelfDroid session remains active after Save.
