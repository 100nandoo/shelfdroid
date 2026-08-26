# 03 — Reconcile Library data during explicit refresh

**What to build:** Explicit Library administration refresh performs Library data synchronization
so changes missed while disconnected converge across both administration and the local Catalog.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] Explicit refresh reconciles Libraries and Library items through the same authoritative
      synchronization boundary used for external Library events.
- [ ] Added, updated, removed, and reordered Libraries converge after refresh even when their
      socket events were missed.
- [ ] A synchronization failure preserves safe error handling and offers retry without displaying
      internal server details.
- [ ] Tests verify convergence after missed events and confirm that refresh does not introduce
      polling.
