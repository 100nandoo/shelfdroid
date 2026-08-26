# 08 — Unify Server task snapshot and socket ingestion

**What to build:** HTTP task snapshots and socket task events pass through one reducer so task
synchronization, notification, retention, and accepted-operation gating cannot diverge between
recovery paths.

**Blocked by:** 07 — Introduce typed operation-agnostic Server task actions.

**Status:** complete

- [x] One reducer preserves completed-task synchronization state for both HTTP and socket input.
- [x] Reducer effects consistently schedule catalog synchronization, one-time notification, and
      terminal-state expiry.
- [x] Reconnection and explicit task refresh retain accepted operation placeholders without
      polling or duplicate delivery.
- [x] Library administration ViewModel tests drive repository state instead of production events
      that exist only to inject connection or task state.
- [x] Behaviour tests cover HTTP/socket ordering, duplicate terminal events, reconnection, and
      synchronization retry.

**Verification:** `:core-data:testDebugUnitTest`, `:core-ui:testDebugUnitTest`,
`:test-app:compileDebugKotlin`, and `:core-ui:compileDebugAndroidTestKotlin` pass. Connected
instrumentation was not run because no Android device or emulator was available.
