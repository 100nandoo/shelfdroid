# 09 — Consolidate create-draft setting transitions

**What to build:** Book and Podcast Library creation use one draft-mutation path and one
finish-threshold state transition while still preserving each media type's hidden setting values.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] Draft mutations consistently mark the form dirty, clear stale validation, and invalidate
      schedule validation through one update boundary.
- [ ] Time-remaining and percent-complete thresholds are mutually exclusive and use one shared
      transition model for Book and Podcast settings.
- [ ] Switching media type preserves the independent hidden values for both media types and submits
      only values applicable to the final type.
- [ ] Existing create-flow behaviour remains unchanged for common, Book-only, Podcast-only, and
      schedule settings.
- [ ] Unit tests cover threshold mode/value changes, media switching, serialization, and validation
      reset behaviour.
