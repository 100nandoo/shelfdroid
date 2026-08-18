# 03 — Configure and discover the OpenID provider

**What to build:** Let an admin configure the OpenID issuer, provider endpoints, client ID, and
signing algorithm, either manually or by asking the Audiobookshelf server to discover provider
metadata. The completed slice validates active OpenID configuration without requiring a live test
login and explains the server restart requirement after saving.

**Blocked by:** 02 — Edit the login message and Login methods.

**Status:** ready-for-human

- [x] The form supports issuer, authorization, token, userinfo, JWKS, logout, client ID, and signing
      algorithm values without exposing or editing the client secret in this ticket.
- [x] Required OpenID fields are validated before OpenID can be enabled or Password sign-in can be
      disabled.
- [x] Auto-populate calls the authenticated Audiobookshelf issuer-discovery endpoint rather than
      contacting the identity provider directly.
- [x] Discovery works for servers installed at the URL root and under a subpath.
- [x] Successful discovery updates provider-owned endpoints and signing algorithm choices while
      preserving client data, callback settings, User-mapping edits, and unrelated draft fields.
- [x] The current signing algorithm is preserved until discovery succeeds and remains selectable
      when supported by the discovered provider.
- [x] Discovery failure preserves the complete draft and identifies discovery as the failed
      operation.
- [x] Manual edits and discovered values participate in existing dirty-state, Reset, partial Save,
      skipped-update, canonical-reload, and unsaved Back behavior.
- [x] Successful saves that change OIDC configuration show a persistent message that the
      Audiobookshelf server must be restarted before all changes take effect.
- [x] Discovery and Save results cannot overwrite admin edits made after those operations began.
- [x] Repository tests cover root and subpath discovery URLs, successful merge behavior, failure
      preservation, algorithm selection, required-field validation, and changed-fields-only Save.
- [x] UI and ViewModel coverage verifies manual editing, discovery loading, discovery failure,
      provider result application, validation, operation serialization, and restart messaging.

## Verification

Automated checks passed:

- `./gradlew :core-data:testDebugUnitTest --tests 'dev.halim.shelfdroid.core.data.screen.authenticationsettings.*'`
- `./gradlew :core-ui:testDebugUnitTest --tests 'dev.halim.shelfdroid.core.ui.screen.authenticationsettings.*'`
- `./gradlew :core-ui:compileDebugAndroidTestKotlin`
- `./gradlew :core-ui:connectedDebugAndroidTest` (5 tests on `Resizable_Baklava`, API 16)
- `git diff --check`

The ViewModel suite includes a regression test confirming Reset preserves the restart warning
after an accepted OIDC-changing save.

The module lint checks remain blocked by pre-existing findings outside this ticket in
`LoginRepository.kt`, `OpenIdCallbackCoordinator.kt`, `EditEpisodeMatchTab.kt`, and
`PreviewWrapper.kt`; no lint findings were reported for the ticket files.

Manual verification remains for an admin/root account against live Audiobookshelf servers at the
URL root and under a subpath, real provider discovery, server restart behavior, and visual
TalkBack/Back behavior.
