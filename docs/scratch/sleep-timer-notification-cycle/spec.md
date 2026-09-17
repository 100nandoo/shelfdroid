# Sleep timer notification cycle

Status: Implemented and reviewed; feature validation passes.

## Behavior

- Add Toggle and Cyclical modes for the sleep-timer media notification action only. Toggle remains the default for existing and new users and retains current start/cancel behavior.
- Cyclical advances through Off and enabled preset durations in ascending order, returning to Off after the longest duration.
- Supported durations: 1, 5, 10, 15, 30, 45, 60 minutes. Exclude custom durations and End of chapter.
- Initially enable 1 and 5 minutes. Require at least one enabled duration; one duration cycles Off → duration → Off.
- Each tap starts the full next duration. Choose the next step from the active timer's original preset, never its remaining countdown.
- A timer started elsewhere advances from its original duration if that duration matches an enabled preset. Otherwise, the first tap cancels it to Off.
- An expired or canceled timer is Off; the next tap starts the shortest enabled duration.
- Changing mode or enabled durations leaves an active timer running. The next tap follows the new mode: Toggle cancels it; Cyclical advances from a still-enabled original preset or cancels an unmatched timer.

## Settings and feedback

- Show the default-duration dropdown in Toggle mode and duration selection chips in Cyclical mode.
- Preserve both mode-specific selections when switching modes.
- Retain the current availability rules tied to the notification action slots.
- Keep existing timer icons. In Cyclical mode, the accessibility label describes the next action, such as “Set sleep timer to 5 minutes” or “Cancel sleep timer”.

## Implementation considerations

- Existing timer state tracks remaining duration only; retain the original selected duration to avoid countdown-dependent cycle behavior.
- Persist mode and enabled durations as local app preferences, with safe defaults for existing installations.
- Preserve existing playback-dependent countdown and expiry behavior.
- Validate preference defaults, cycle transitions, timers started elsewhere, settings changes during an active timer, and the mode-specific settings UI.

## Validation

- Agreed test boundaries: PlayerStore notification timer actions, NotificationPrefs serialization, and SettingsNotificationContent controls.
- Full command: `./gradlew testDebugUnitTest connectedDebugAndroidTest --continue --console=plain`.
- All 609 unit tests pass. Device tests: 100 pass and 5 fail, including all 10 notification settings tests and all 6 PlayerStore timer tests passing.
- The same five failures and messages reproduce on the unchanged baseline commit `5daefe923fb6bf675ae6c2a1f8eb5a749065929a` with `./gradlew :test-app:connectedDebugAndroidTest` in an isolated checkout:
  - Four AppTest login/navigation checks fail with “No compose hierarchies found in the app”.
  - LibraryAdminRepositoryTest's partial-success reorder check expects `podcasts, books` but finds `lib-book, lib-podcast`. It passes in a filtered run and fails in the full app suite on both versions.
- Standards review: no actionable findings.
- Spec review: no blocking findings; the requested real-playback expiry check was added and passes.
