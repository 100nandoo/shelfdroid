# 01: Make Chapter navigation safe and session-addressable

**What to build:** Make ShelfDroid's expanded player expose safe, consistent Previous Chapter and Next Chapter behavior through the MediaSession so the existing UI works correctly and another same-app playback surface can issue the same commands without duplicating domain logic.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] Previous Chapter restarts the current Chapter when playback is more than three seconds into it.
- [ ] Previous Chapter selects the preceding Chapter when playback is at or before three seconds and a preceding Chapter exists.
- [ ] Previous restarts the playable unit on the first Chapter, a Book without Chapters, a single-Chapter Book, and a Podcast Episode.
- [ ] Next Chapter advances only when a valid next Chapter exists.
- [ ] Next Chapter is unavailable on the final Chapter, a Book without Chapters, a single-Chapter Book, and a Podcast Episode.
- [ ] The expanded player keeps both controls in stable positions and disables unavailable actions instead of hiding the player or entering an error state.
- [ ] Previous Chapter and Next Chapter are exposed as custom MediaSession commands only to trusted same-app controllers.
- [ ] Native previous/next media-item commands are not substituted for ShelfDroid Chapter behavior.
- [ ] Automatic end-of-Chapter progression continues to work for multi-Chapter Books.
- [ ] JVM tests cover the restart threshold, valid transitions, boundaries, podcasts, no-Chapter Books, and single-Chapter Books through the shared public command/availability seam.

