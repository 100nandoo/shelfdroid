# 06 — Reorder Libraries

**What to build:** An administrator can change Library order with immediate feedback and recover
safely if the server rejects the change.

**Blocked by:** 02 — Browse Library administration; 03 — Create a Library from its Details.

**Status:** complete

- [x] Libraries can be reordered by drag and by accessible move-up/move-down actions.
- [x] The list updates optimistically and rolls back visibly if persistence fails.
- [x] Reorder participates in global create/delete/reorder serialization.
- [x] Reorder is unavailable for a Library with an active or unknown Server-task state.
- [x] The last complete order accepted by the server wins after overlapping user intent or refresh.
- [x] Accepted order is reflected by the catalog and survives local persistence through
      `displayOrder` without caching rich administration configuration.
- [x] Tests cover drag, accessible actions, boundaries, serialization, rollback, task gating, and
      server-authoritative reconciliation.
