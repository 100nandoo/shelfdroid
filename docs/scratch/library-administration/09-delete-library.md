# 09 — Delete a Library safely

**What to build:** An administrator can remove an idle Library from the catalog with an explicit
understanding that its media files remain intact.

**Blocked by:** 03 — Create a Library from its Details; 07 — Scan a Library with durable Server-task
tracking.

**Status:** complete

- [x] An idle Library exposes a direct tonal Delete action with tooltip and content description.
- [x] Confirmation states that catalog data is removed and media files are retained.
- [x] Delete is disabled while task state is unknown/disconnected or that Library has an active
      task, and it participates in global mutation serialization.
- [x] Success immediately removes the Library from administration and reconciles the local catalog.
- [x] Deleting the active Library activates the next available Library or the empty state when none
      remains.
- [x] Existing buffered playback is not forcibly stopped solely because the Library was deleted.
- [x] Failure preserves the Library and reports a safe, actionable error.
- [x] Tests cover confirmation, cancellation, task gating, serialization, active-Library fallback,
      final-Library deletion, playback continuity, success, and failure.
