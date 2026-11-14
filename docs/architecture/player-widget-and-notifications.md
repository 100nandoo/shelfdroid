# Player widget and notification behavior

This guide collects the implemented behavior recorded in the completed feature specs. The
[archived specs](../archive/README.md) preserve their full decisions and implementation evidence.

## Player widget

- The widget follows ShelfDroid's dark-mode and dynamic-color preferences. Appearance changes
  refresh placed widgets even while playback is idle.
- Layout responds to available widget dimensions. Compact widgets show cover, metadata, and
  rewind/play-pause/forward controls; larger layouts allow more cover and chapter information.
- Larger covers remain square, keep the full artwork visible, and grow within the space left by
  metadata and playback controls.
- Settings offers an Add widget action. The pin preview uses current playback when available
  and sample content otherwise; request failures appear in a snackbar.
- Widget-picker previews use bundled Sherlock Holmes sample content, independently of account
  state and playback. Generated previews are available on Android 15 and later.

The [responsive layout record](../archive/widget-responsive-layouts/spec.md) describes the earlier
chapter-control placement. The [unified control-row spec](../archive/widget-unified-control-row/spec.md)
revises it and is completed. It keeps chapter and playback buttons in a
single row where chapter controls apply. The [cover-growth record](../archive/widget-large-cover-growth/spec.md)
builds on that placement. Both feature specs are archived as completed.

## Media notification

Player presentation (Mini or Expanded) and opening screen (Home or Media details) are independent
preferences. Home is the default opening screen. Notification taps apply the chosen opening screen
for both fresh launches and an already running app, replacing previous browsing history.

The playback-speed action supports Cyclical and Toggle modes. Cyclical remains the default; Toggle
switches between 1× and one selected target speed. Editing the configuration changes the next
notification action rather than immediately changing playback speed.

The sleep-timer action supports Toggle and Cyclical modes. Toggle remains the default. Cyclical
advances through Off and enabled preset durations in ascending order, restarting the full duration
at each step. Changing configuration leaves an active timer running until the next action.

See the archived [notification opening-screen](../archive/media-notification-opening-screen/spec.md),
[playback-speed](../archive/playback-speed-notification-toggle/spec.md), and
[sleep-timer](../archive/sleep-timer-notification-cycle/spec.md) specs for edge cases and defaults.
