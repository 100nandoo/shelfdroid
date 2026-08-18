# 02 — Edit the login message and Login methods

**What to build:** Turn the read-only overview into a saveable Authentication form for the custom
login message, Password sign-in, and OpenID login. Admins can preview server HTML, reset drafts,
confirm dangerous changes, and save a changed-fields-only update without leaving the server with
no usable Login method.

**Blocked by:** 01 — Admin-only Authentication settings overview.

**Status:** ready-for-human

- [x] The custom login message can be enabled, edited as HTML source, rendered with the same
      semantics as Login, disabled, and reset to the last canonical server snapshot.
- [x] Existing complex HTML remains unchanged when the admin does not edit the message.
- [x] Disabling or blanking the custom message submits an explicit clear rather than omitting the
      field.
- [x] Password sign-in and OpenID login switches reflect and update only the supported `local` and
      `openid` Login methods.
- [x] Incomplete OpenID configuration can be saved while OpenID remains disabled.
- [x] The form rejects a state with no active Login method.
- [x] Disabling Password sign-in requires confirmation and is blocked unless enabled OpenID
      configuration is structurally valid.
- [x] Save is enabled only when the draft is valid, dirty, and not already saving; Reset restores
      the complete saved snapshot.
- [x] Back warns before discarding unsaved changes and clears transient operation state when the
      admin confirms leaving.
- [x] Saving sends only changed fields, preserves untouched server values, and uses the dedicated
      partial-update endpoint.
- [x] A non-empty update that returns `updated: false` is surfaced as rejected or skipped rather
      than successful.
- [x] An accepted update reloads canonical Authentication settings and establishes a clean saved
      snapshot.
- [x] Repository and ViewModel tests cover load, edit, clear, reset, validation, confirmation,
      no-op, partial Save, skipped update, canonical reload, and unsaved Back behavior.
- [x] Integration coverage confirms Login discovery observes the saved custom message and active
      Login methods after the server applies them.

## Verification

Automated checks passed:

- `./gradlew :core-data:testDebugUnitTest :core-ui:testDebugUnitTest
  :core-ui:compileDebugAndroidTestKotlin`
- `./gradlew :core-data:testDebugUnitTest --tests '*AuthenticationSettingsRepositoryTest'
  --tests '*AuthenticationSettingsFormTest'
  --tests '*AuthenticationSettingsLoginDiscoveryIntegrationTest'
  :core-ui:testDebugUnitTest --tests '*AuthenticationSettingsViewModelStateTest'
  --tests '*MiscScreenTest'`
- `git diff --check`

The broader `./gradlew :core-data:check :core-ui:check` check remains blocked by pre-existing lint
errors in `LoginRepository.kt`, `OpenIdCallbackCoordinator.kt`, `EditEpisodeMatchTab.kt`, and
`PreviewWrapper.kt`; no lint findings were reported for the ticket files. Instrumentation could
not run locally because there is no connected device, but the editor test source compiles with
`./gradlew :core-ui:compileDebugAndroidTestKotlin`.

Manual verification remains for the editor and confirmation dialogs on an admin/root account,
HTML rendering parity with Login, Back behavior, and saving against root and URL-subpath
Audiobookshelf installations.
