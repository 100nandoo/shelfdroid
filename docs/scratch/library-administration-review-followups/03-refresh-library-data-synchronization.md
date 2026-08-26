# 03 — Reconcile Library data during explicit refresh

**What to build:** Explicit Library administration refresh performs Library data synchronization
so changes missed while disconnected converge across both administration and the local Catalog.

**Blocked by:** None — can start immediately.

**Status:** complete

- [x] Explicit refresh reconciles Libraries and Library items through the same authoritative
      synchronization boundary used for external Library events.
- [x] Added, updated, removed, and reordered Libraries converge after refresh even when their
      socket events were missed.
- [x] A synchronization failure preserves safe error handling and offers retry without displaying
      internal server details.
- [x] Tests verify convergence after missed events and confirm that refresh does not introduce
      polling.

**Verification:** `:core-data:testDebugUnitTest` and `:core-ui:testDebugUnitTest` pass, including
Library data ordering, missed-event convergence, safe refresh failure, and no-polling coverage.
The direct orphaned-Library-item instrumentation test compiles with
`:test-app:compileDebugKotlin`; connected instrumentation was not run because no Android device
or emulator was available (`adb devices` returned no devices).
