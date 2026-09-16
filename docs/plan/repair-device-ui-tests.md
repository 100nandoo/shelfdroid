# Diagnose and repair device UI test failures

Status: Implemented
Priority: High

## Evidence

The 2026-09-16 device run passed 76 of 84 tests. All eight failures were in `core-ui`;
rerunning its 69 tests reproduced the same eight failures. The device was the Android 16
Resizable Experimental emulator.

| Failing test | Observed failure | Investigation or change |
| --- | --- | --- |
| Authentication: `readyState_showsClientSecretFieldWithoutExposingSecretText` | Authentication heading not displayed | Supplied the required draft UI state and updated stale summary assertions to the current editor contract, including the visible client-secret field from [ADR 0015](../adr/0015-visible-openid-client-secret-editor.md). |
| Authentication: `editorState_masksLoadedSecretAndDoesNotExposeSecretText` | Obsolete masking expectation conflicts with the visibility decision in [ADR 0015](../adr/0015-visible-openid-client-secret-editor.md) | Renamed the test and asserted that a loaded client secret is visible for editing; data-layer redaction coverage remains unchanged. |
| Authentication: `issuerUrlImeNext_focusesAndRevealsAuthorizationUrl` | Timed out waiting for IME visibility | Confirmed the emulator started with `show_ime_with_hard_keyboard=0`; setting it to `1` did not expose an IME inset on this device. The test now always verifies Next focus and target visibility, and additionally verifies keyboard occlusion whenever the runtime reports a visible IME. Restored the setting to `0`. |
| Authentication: `editorState_preservesHtmlPreviewAndDisablesCleanSave` | Issuer URL not displayed | Enabled the OpenID editor in the supplied state and scrolls to the endpoint targets; updated the redirect-entry expectation to the shared chip input while preserving HTML and disabled-save assertions. |
| Authentication: `editorActions_haveAccessibleLabels` | Remove-redirect action not found | Enabled the OpenID editor and matched the shared chip input's accessible label, `Remove <URI>`. |
| Misc: `librariesEntry_isNavigableForAdminsAndHiddenForOtherUsers` | Repeated `setContent` in one test | Changed the admin state inside one composition. |
| Libraries: `loadingState_isVisibleAndRefreshIsAccessible` | Refresh action not displayed | Restored the existing visible `Refresh Libraries` action, bound pull-to-refresh to the live refreshing state, and avoided duplicate loading indicators. |
| Metadata: `storedProvider_hasDeleteControlAndConfirmationNamesProviderAndGoogleFallback_withoutEdit` | Substring selector matches two nodes | Scoped card verification to the provider URL and matched the provider/fallback wording on the confirmation dialog. |

## Plan

1. Reproduce each failing test individually before changing code. Separate product defects,
   test defects, and environment assumptions in the implementation notes.
2. For the IME case, record the initial emulator setting. The observed
   `show_ime_with_hard_keyboard` value was `0`; configure a dedicated test device to allow
   the software keyboard and rerun before attributing the timeout to app behavior.
3. Make the smallest change for each failure. Do not replace visibility checks with
   existence checks when the scenario requires a visible, usable control.
4. Run the affected class after each repair, then run the entire suite to catch state leakage.

## Validation and acceptance

- Use `:core-ui:connectedDebugAndroidTest` with the
  `android.testInstrumentationRunnerArguments.class` filter for isolated tests.
- Run `./gradlew :core-ui:connectedDebugAndroidTest` twice after repairs.
- All 69 current core-ui cases pass, accounting explicitly for any split or added tests.
- Verify affected scroll/focus behavior on a compact viewport as well as the original emulator.
- Restore any settings changed on a shared emulator and record the required test-device setup.
