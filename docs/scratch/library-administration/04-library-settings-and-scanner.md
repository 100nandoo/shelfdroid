# 04 — Configure Library Settings and Scanner precedence

**What to build:** An administrator can configure website-parity Library settings and book scanner
precedence as part of creation.

**Blocked by:** 03 — Create a Library from its Details.

**Status:** complete

- [x] Book creation exposes applicable cover, watcher, audiobook, identifier, series, EPUB, and
      finish-threshold settings with website-equivalent defaults.
- [x] Podcast creation exposes applicable cover, watcher, region, and finish-threshold settings and
      does not show a Scanner tab.
- [x] Book creation exposes all six scanner metadata sources, including enable/disable and reorder.
- [x] Displayed scanner priority and submitted precedence preserve the website's ordering semantics.
- [x] Changing media type preserves hidden draft values and submits only fields applicable to the
      final type.
- [x] Enabling scripted EPUB support shows a supporting warning without requiring confirmation.
- [x] Settings and scanner validation routes focus to the relevant bottom tab and remain accessible.
- [x] Tests cover defaults, media switching, hidden-value preservation, serialization, scanner
      precedence, and scripted-EPUB warning behaviour.
