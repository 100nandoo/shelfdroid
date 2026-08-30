# 04: Control primary playback from the widget

**What to build:** Let a listener play, pause, seek backward, and seek forward from either widget size while keeping each displayed action synchronized with the authoritative Media3 session and failing safely if playback disappears.

**Blocked by:** 03: Show Current playback in the widget.

**Status:** ready-for-agent

- [ ] Both responsive layouts show seek backward, play/pause, and seek forward in stable positions for controllable Current playback.
- [ ] Play, pause, seek backward, and seek forward use their standard Media3 Player commands rather than custom session commands.
- [ ] Seek backward and seek forward use ShelfDroid's existing 10-second increments.
- [ ] Pause is displayed while playback is active or buffering with an intent to continue.
- [ ] Play is displayed while playback is paused or ended.
- [ ] Each action connects through a short-lived asynchronous MediaController, dispatches the command to the playback service, and releases the controller without dropping pending commands.
- [ ] A successful action requests a prompt widget refresh without making optimistic widget state authoritative.
- [ ] If no current media remains when a control action runs, the action opens ShelfDroid normally instead of silently failing or resuming stale media.
- [ ] Playback errors show the accepted recovery affordance instead of enabled transport controls.
- [ ] Controls use ShelfDroid's Rounded icon style, expose meaningful content descriptions, and communicate disabled state where supported.
- [ ] JVM tests using a fake controller/session gateway verify dispatched commands, playback-intent mapping, error behavior, missing-playback fallback, and post-command refresh requests.

