# 05: Add Chapter controls to the large widget

**What to build:** Give listeners safe Previous Chapter and Next Chapter controls in the large playback widget while keeping the small widget focused on its three primary controls.

**Blocked by:** 01: Make Chapter navigation safe and session-addressable; 04: Control primary playback from the widget.

**Status:** ready-for-agent

- [ ] The 4×2-or-wider layout shows Previous Chapter and Next Chapter alongside the three primary controls.
- [ ] The 3×2 layout continues to show only seek backward, play/pause, and seek forward.
- [ ] Widget Chapter actions use the shared custom MediaSession commands and do not call playback repositories or stores directly.
- [ ] Previous Chapter follows the shared restart-versus-previous threshold and remains useful on the first Chapter, no-Chapter Books, single-Chapter Books, and Podcast Episodes by restarting the playable unit.
- [ ] Next Chapter is enabled only when a valid next Chapter exists.
- [ ] Next Chapter remains visible but disabled on the final Chapter, no-Chapter Books, single-Chapter Books, and Podcast Episodes.
- [ ] Chapter control positions remain stable as availability changes.
- [ ] Chapter actions request a prompt refresh after dispatch while the MediaSession remains the source of truth.
- [ ] Chapter controls use ShelfDroid's Rounded icon style and have unambiguous accessibility descriptions that distinguish Chapter navigation from 10-second seeking.
- [ ] Glance structural tests verify the three-control and five-control size variants plus enabled and disabled Chapter states.
- [ ] Action tests verify that each large-widget Chapter control sends the corresponding shared custom command.

