# 07 — Introduce typed operation-agnostic Server task actions

**What to build:** Server tasks expose a typed action classification that gives Library scan and
Book matching one consistent presentation while retaining the raw identity of unknown future
Audiobookshelf server task actions.

**Blocked by:** None — can start immediately.

**Status:** complete

- [x] Server task action parsing recognizes Library scan and Book matching without scattering raw
      string comparisons through repositories and UI.
- [x] Unknown actions remain representable so the application-scoped repository stays
      operation-agnostic as required by the shared Server task ADR.
- [x] Task rows and completion notifications use one action/status presentation mapping.
- [x] Server task models and contracts are separated from the repository so its file contains only
      the repository class.
- [x] Mapping, presentation, and notification tests cover known and unknown actions.

**Verification:** `:core-data:testDebugUnitTest`, `:core-ui:testDebugUnitTest`,
`:test-app:compileDebugKotlin`, and `:core-ui:compileDebugAndroidTestKotlin` pass. Connected
instrumentation was not run because no Android device or emulator was available.
