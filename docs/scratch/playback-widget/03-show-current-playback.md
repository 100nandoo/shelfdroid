# 03: Show Current playback in the widget

**What to build:** Show the listener which Book, Podcast, Chapter, or Episode is represented by the live Current playback, with useful artwork and navigation into Now Playing while retaining a safe empty or error presentation when the session cannot supply controllable media.

**Blocked by:** 02: Add the responsive empty playback widget.

**Status:** ready-for-agent

- [ ] The Glance widget remains stateless and passive and obtains a fresh presentation snapshot from the live Media3 session rather than trusting widget-local memory.
- [ ] A stopped playback service or a session without a current media item renders the accepted empty state.
- [ ] Playing, paused, buffering, and ended media with a loaded item render as Current playback.
- [ ] Current cover art is loaded through ShelfDroid's authenticated or cached image facilities and downsampled appropriately for the widget.
- [ ] A ShelfDroid placeholder is shown when current cover art is unavailable, and artwork from a previous item is never retained after playback clears.
- [ ] The 3×2 layout shows the cover without title, Chapter, or Episode text.
- [ ] The 4×2-or-wider layout shows the audiobook or podcast title and the current Chapter or Episode title on two compact, ellipsized lines.
- [ ] Tapping active artwork, metadata, or otherwise unused widget surface opens Now Playing for Current playback.
- [ ] A playback error retains identifying cover and metadata but presents an “Open ShelfDroid” recovery affordance instead of pretending playback is controllable.
- [ ] The recovery affordance opens Now Playing for the affected Current playback.
- [ ] Content descriptions identify artwork, metadata, empty state, and recovery actions without relying on visual context.
- [ ] Glance JVM structural tests cover active, empty, error, cover-fallback, 3×2, and 4×2-or-wider presentations and their navigation actions.

