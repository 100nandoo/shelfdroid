# 07 — Scan a Library with durable Server-task tracking

**What to build:** An administrator can start a normal Library scan and follow its actual server
execution independently of the initiating screen.

**Blocked by:** 01 — Make shared socket ownership safe; 02 — Browse Library administration.

**Status:** complete

- [x] A direct tonal Scan action with tooltip and content description is available for idle Book
      and Podcast Libraries.
- [x] HTTP acceptance is represented as started, not completed; completion comes from Server-task
      state.
- [x] An application-scoped, operation-agnostic repository bootstraps tasks over HTTP and reconciles
      task socket events by Library without polling.
- [x] Scan state survives navigation and is restored after socket reconnection or manual refresh by
      reloading the task snapshot.
- [x] Scan-sensitive actions are disabled while state is unknown, disconnected, or active.
- [x] Completion distinguishes completed, failed, and cancelled and shows available counts and
      elapsed time for one minute plus a one-time snackbar.
- [x] Successful scanning followed by failed catalog synchronization is reported separately and can
      retry synchronization without rerunning the scan.
- [x] Safe server errors may be shown; internal details are replaced by localized generic errors.
- [x] Tests cover acceptance, task events, navigation, reconnection, refresh, no polling, gating,
      result expiry, snackbar deduplication, failure, and synchronization retry.
