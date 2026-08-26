# 08 — Unify Server task snapshot and socket ingestion

**What to build:** HTTP task snapshots and socket task events pass through one reducer so task
synchronization, notification, retention, and accepted-operation gating cannot diverge between
recovery paths.

**Blocked by:** 07 — Introduce typed operation-agnostic Server task actions.

**Status:** ready-for-agent

- [ ] One reducer preserves completed-task synchronization state for both HTTP and socket input.
- [ ] Reducer effects consistently schedule catalog synchronization, one-time notification, and
      terminal-state expiry.
- [ ] Reconnection and explicit task refresh retain accepted operation placeholders without
      polling or duplicate delivery.
- [ ] Library administration ViewModel tests drive repository state instead of production events
      that exist only to inject connection or task state.
- [ ] Behaviour tests cover HTTP/socket ordering, duplicate terminal events, reconnection, and
      synchronization retry.
