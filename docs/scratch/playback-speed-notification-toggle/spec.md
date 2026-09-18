# Playback speed notification toggle

Status: Implemented and reviewed.

## Behavior

- Add Toggle and Cyclical modes for the playback-speed media notification action only.
- Keep Cyclical as the default for existing and new installations, preserving current cycling behavior.
- Toggle switches between 1× and one listener-selected target speed. If current speed matches the target, the next tap sets 1×; otherwise it sets the target, including when current speed matches neither endpoint.
- Toggle target choices: 0.5, 0.75, 1.25, 1.5, 1.75, and 2×. Default target: 1.5×. Exclude 1× because it would make Toggle ineffective.
- Use the existing speed comparison tolerance when determining whether current speed matches the target.
- Preserve the existing Cyclical presets, ascending order, minimum of two selected speeds, normalization, and next-speed/wrap behavior.
- Changing mode, target, or cycle selections does not immediately change playback speed. The next notification tap uses the updated configuration.
- Other playback-speed controls retain their existing behavior.

## Settings and feedback

- In SettingsNotificationScreen, mirror the sleep-timer mode selector with Toggle and Cyclical choices.
- Show a single target-speed picker in Toggle mode and the existing speed selection chips in Cyclical mode.
- Preserve both mode-specific selections when switching modes.
- Keep the existing availability rule: enable playback-speed settings only when Playback speed occupies a notification action slot.
- Keep the notification icon representing current speed.
- Make the accessibility label describe the next action in either mode, for example “Set playback speed to 1.5×”.

## Persistence and validation

- Persist mode and target as local app preferences alongside the existing cycle selection, with safe defaults for legacy JSON and invalid target values.
- Validate legacy preference decoding, defaults, and independent mode-specific selection persistence.
- Validate Toggle transitions at 1×, at the target, and at an arbitrary speed; target matching tolerance; updated settings; and unchanged Cyclical transitions.
- Validate settings mode controls, target choices, preserved selections, action-slot availability, current-speed icons, and next-action accessibility labels.
- Run the relevant preference, player, and notification settings checks before declaring implementation complete.

## Domain documentation

- CONTEXT.md defines Playback speed notification mode alongside Playback speed cycle.
- No ADR is needed: this follows the existing timer pattern and is straightforward to reverse.
