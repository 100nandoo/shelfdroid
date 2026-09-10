# Configurable media notification actions

Status: implemented.

## Confirmed

- Add a playback-speed media notification action.
- Tapping the playback-speed action cycles through a chosen list of speeds.
- Let listeners select and order the custom media notification actions, including sleep timer, next chapter, and playback speed.
- Put configuration in `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/settings/notification/SettingsNotificationScreen.kt`.
- Provide two configurable positions, each offering Timer, Next chapter, Playback speed, or None. Do not allow duplicate actions; both positions may be None.
- Preserve the default order: Timer, then Next chapter.
- Preserve existing next-chapter availability: hide it for podcasts and books without multiple chapters; disable it on the final chapter or during a chapter transition. Do not substitute another action.
- Apply preferences immediately to the current notification and share them across books and podcasts.
- Configure the speed cycle with Material 3 `FilterChip` components in a wrapping `FlowRow`, selecting from the same predefined speeds as the player: 0.5×, 0.75×, 1×, 1.25×, 1.5×, 1.75×, and 2×. Do not allow arbitrary speed entry.
- After the fastest selected speed, wrap to the slowest selected speed.
- Keep the main player's speed control unchanged; do not add custom-value support.
- Cycle selected speeds in ascending order. Require at least two selected speeds and default to 1×, 1.25×, 1.5×, and 2×.
- Each tap advances to the closest strictly higher selected speed. If there is none, use the slowest selected speed. This also applies when the current speed is not selected: with choices [1×, 1.5×, 2×], current 1.25× advances to 1.5×.

## Open decisions

None.

## Existing behavior to account for

- The player speed slider currently offers 0.5–2 in 0.25 increments.
- Notification speed changes must keep actual playback and the open player UI synchronized.
- Preserve existing speed retention settings across media changes.
- Verify platform button ordering and presentation constraints before implementation.
- Material 3 `FilterChip` is used for the predefined speed choices, with selected chips showing a checkmark and the final two selected chips disabled to enforce the minimum.

Implementation is complete in the notification preferences, settings UI, media session callback, and playback service.
