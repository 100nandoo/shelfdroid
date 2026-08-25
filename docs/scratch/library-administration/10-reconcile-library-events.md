# 10 — Reconcile external Library changes

**What to build:** Library administration and the local catalog stay current when another client
adds, updates, or removes a Library.

**Blocked by:** 01 — Make shared socket ownership safe; 02 — Browse Library administration.

**Status:** complete

- [x] Library-added events insert the new Library at the server-defined position and reconcile the
      catalog.
- [x] Library-updated events refresh visible administration data and catalog-relevant state without
      caching rich administration configuration.
- [x] Library-removed events remove the Library and apply the same active-Library fallback semantics
      as an in-app deletion.
- [x] Events coexist with podcast and Server-task subscriptions without listener replacement or
      premature socket disconnection.
- [x] Duplicate, missed, or out-of-order event effects converge through explicit refresh.
- [x] Tests cover add, update, remove, active-Library fallback, subscription coexistence, duplicate
      delivery, and refresh reconciliation.
