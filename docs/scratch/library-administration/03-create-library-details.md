# 03 — Create a Library from its Details

**What to build:** An administrator can create a Book or Podcast Library from the Details portion
of a reusable, bottom-tabbed create flow.

**Blocked by:** 02 — Browse Library administration.

**Status:** complete

- [x] A full-width Create action opens a flow with bottom-aligned scrollable tabs and a persistent
      submission action.
- [x] Details supports media type, trimmed name, the Audiobookshelf icon identifiers rendered with
      Rounded assets, and a dynamically loaded provider appropriate to the media type.
- [x] Provider load failure is visible and retryable and prevents submission until resolved.
- [x] Folders can be selected through a server filesystem browser that supports POSIX and Windows
      paths or entered manually, including paths the server has not created yet.
- [x] Validation rejects blank names, no folders, duplicates, and parent/child-overlapping folders;
      invalid submission focuses the first invalid field and exposes accessible inline errors.
- [x] Back navigation with a changed draft requests confirmation before discarding it.
- [x] Only one create/delete/reorder mutation can be active globally.
- [x] Success returns to administration with the new Library visible and reconciles the local
      catalog; server and local-sync failures are distinguished and safely reported.
- [x] Functional and accessibility tests cover both media types, providers, folder entry/browsing,
      validation, unsaved changes, successful creation, and failure recovery.
