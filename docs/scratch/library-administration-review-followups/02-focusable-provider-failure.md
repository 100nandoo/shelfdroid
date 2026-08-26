# 02 — Make provider-loading failures focusable and retryable

**What to build:** When Library provider loading fails, invalid create submission moves
accessibility focus to a visible retryable error target instead of requesting focus from a control
that is not present.

**Blocked by:** None — can start immediately.

**Status:** complete

- [x] Provider loading, failure, retry, and success states each expose a valid focus target when
      they can be selected as the first invalid field.
- [x] Submitting after provider loading fails does not throw and selects the Details tab.
- [x] The focused failure state clearly announces the error and retry action to accessibility
      services.
- [x] Instrumentation tests cover invalid submission during provider failure and successful retry.

**Verification:** Unit tests and Android test compilation pass. Connected instrumentation was not
run because no Android device or emulator was available (`adb devices` returned no devices).
