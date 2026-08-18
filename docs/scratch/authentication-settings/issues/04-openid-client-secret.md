# 04 — Rotate the OpenID client secret securely

**What to build:** Add a security-focused client-secret workflow that tells admins whether a secret
is configured and lets them replace or explicitly clear it without redisplaying the server's raw
value or retaining replacement text after it is needed.

**Blocked by:** 03 — Configure and discover the OpenID provider.

**Status:** ready-for-human

- [x] Loaded state exposes only configured/not-configured, and the raw response value does not enter
      form, persistence, navigation, preview, log, analytics, or accessibility state.
- [x] Replacement input is masked by default and supports a temporary reveal action.
- [x] Secret update intent distinguishes untouched, replace, and explicit clear.
- [x] Untouched intent omits the secret from every update, including saves of unrelated
      Authentication settings.
- [x] Replacement intent sends entered text exactly once and never substitutes a visual mask.
- [x] Clear intent requires explicit confirmation and sends the server's supported explicit-clear
      representation.
- [x] Secret replacement text is cleared after success, failure, Reset, access denial, confirmed
      Back, and navigation away.
- [x] Save validation prevents clearing required secret configuration when doing so would leave
      enabled OpenID login structurally invalid.
- [x] Repository serialization tests prove untouched omission, replacement, explicit clear, and
      post-save canonical configured state.
- [x] ViewModel and UI tests cover masking, temporary reveal, clear confirmation, cleanup paths,
      and absence of secret text from previews and accessibility output.
- [x] Screen-capture and recent-app-preview behavior is unchanged by this ticket.
