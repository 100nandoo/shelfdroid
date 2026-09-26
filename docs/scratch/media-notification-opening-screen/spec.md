# Media notification opening screen

Status: Implemented; notification presentation regression fixed and verified.

Extends commit `653e62f557e611e0a724b68f5b0088d6c211a2b4`.

## Confirmed decisions

- Offer two independent notification preferences: Player presentation (Mini / Expanded) and Opening screen (Home / Media details), allowing all four combinations.
- Default Opening screen to Home for both existing and new installations. Existing users without this preference move to Home behavior.
- Apply the selected opening screen on both fresh launches and notification taps while the app is already running, replacing previous browsing history.
- Home produces a Home-only screen history beneath the selected player presentation.
- Media details preserves the existing screen history: Home > Book detail, or Home > Podcast detail > Episode detail.
- The player presentation is separate from screen navigation history.
- Use Media notification player presentation and Media notification opening screen as distinct glossary terms.
- Home retains its ordinary selected library/page; notification taps do not switch to the playing media's library.
- Preserve existing Mini / Expanded preferences and keep Expanded as the default player presentation.
- Back from Expanded collapses to Mini on the chosen opening screen. Back from Mini follows ordinary screen navigation.
- Preference changes affect the next media-notification tap; changing a preference does not navigate or change the currently visible player presentation.
- Apply the opening-screen preference only to media-notification taps. Other entry points retain their existing behavior.
- Fall back to Home when the notification has no media ID. Show the player only when current playback exists.
- Preserve the existing authentication gate: notification navigation waits until the listener is logged in.

## Confirmation

All interview decisions are settled and implemented.

## Validation

- Unit tests cover legacy preference decoding, all four opening-screen/player-presentation combinations for Books and Episodes, and missing or invalid notification extras.
- Navigation tests verify Home-only and Media details stacks, plus retention of an existing `Home(true)` entry while notification replacement clears the detail history.
- The notification settings UI instrumentation test passed: `./gradlew :core-ui:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dev.halim.shelfdroid.core.ui.screen.settings.notification.SettingsNotificationContentTest`.
- Emulator regression tests send a retained tap `PendingIntent` after switching Mini to Expanded and Expanded to Mini; both resolve to the updated presentation.
- The full unit suite passed: `./gradlew test`.
- The media module compiled successfully with `:media:compileDebugKotlin`.
- The emulator regression sends the retained `PendingIntent` directly; a full test that taps the published Media3 notification while ShelfDroid is backgrounded, checks cold and warm activity launches, or asserts the visible player and Home page was not run. Intent parsing, resolved stacks, and Home-root retention are covered by unit tests; end-to-end player transitions and Back handling are not.

## Documentation

- Glossary terms are recorded in [CONTEXT.md](../../../CONTEXT.md).
- No ADR is proposed: the current choices are straightforward to reverse and do not introduce an architectural constraint.
