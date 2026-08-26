# 02 — Make provider-loading failures focusable and retryable

**What to build:** When Library provider loading fails, invalid create submission moves
accessibility focus to a visible retryable error target instead of requesting focus from a control
that is not present.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] Provider loading, failure, retry, and success states each expose a valid focus target when
      they can be selected as the first invalid field.
- [ ] Submitting after provider loading fails does not throw and selects the Details tab.
- [ ] The focused failure state clearly announces the error and retry action to accessibility
      services.
- [ ] Instrumentation tests cover invalid submission during provider failure and successful retry.
