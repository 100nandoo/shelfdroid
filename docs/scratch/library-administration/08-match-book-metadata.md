# 08 — Match missing Book metadata

**What to build:** An administrator can fill missing Book metadata using the same durable
Server-task experience as scanning.

**Blocked by:** 07 — Scan a Library with durable Server-task tracking.

**Status:** complete

- [x] Book Libraries expose a direct tonal Match action with tooltip and content description;
      Podcast Libraries do not.
- [x] Match and Scan are mutually exclusive for the same Library but do not prevent tasks on other
      Libraries.
- [x] Match uses the generic Server-task repository and remains accurate across navigation,
      disconnection, reconnection, and explicit refresh without polling.
- [x] Completion distinguishes completed, failed, and cancelled and displays available counts and
      elapsed time for one minute plus a one-time snackbar.
- [x] Successful matching followed by failed local synchronization is reported separately and can
      retry synchronization without starting another match.
- [x] Tests cover media-type visibility, per-Library concurrency, task recovery, results, failures,
      and synchronization retry.
