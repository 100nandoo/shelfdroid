# 04 — Preserve accepted Library reordering after synchronization failure

**What to build:** Once the Audiobookshelf server accepts a Library order, ShelfDroid keeps that
order visible and reports any later local synchronization failure as a distinct retryable outcome
instead of rolling back the accepted mutation.

**Blocked by:** None — can start immediately.

**Status:** complete

- [x] Reorder results distinguish server rejection from server acceptance followed by failed
      Library data synchronization.
- [x] Server rejection restores the last server-authoritative order, while local synchronization
      failure retains the newly accepted order.
- [x] Retrying a partial success performs only Library data synchronization and never repeats the
      reorder request.
- [x] State and failure tests cover acceptance, rollback, partial success, and synchronization
      retry.

**Verification:** `:core-data:testDebugUnitTest`, `:core-ui:testDebugUnitTest`, and
`:test-app:compileDebugKotlin` pass. Focused ViewModel tests cover server rejection rollback,
accepted-order projection, partial-success state, failed synchronization retry, and no repeated
reorder request. Connected instrumentation was not run because no Android device or emulator was
available.
