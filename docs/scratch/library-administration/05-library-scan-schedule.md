# 05 — Schedule automatic Library scans

**What to build:** An administrator can configure and understand an automatic scan schedule while
creating a Library.

**Blocked by:** 03 — Create a Library from its Details.

**Status:** complete

- [x] The Schedule tab supports disabled scheduling and website-equivalent presets.
- [x] Weekday/time and interval controls produce the expected schedule expression.
- [x] Advanced mode accepts a five-field cron expression and validates it with the server.
- [x] Valid schedules show a human-readable description and next run.
- [x] Invalid or unavailable validation shows an accessible inline error and prevents submission.
- [x] Switching schedule modes preserves intentional draft state without submitting stale fields.
- [x] Tests cover disabled, preset, weekday/time, interval, advanced, invalid, and server-failure
      cases.
