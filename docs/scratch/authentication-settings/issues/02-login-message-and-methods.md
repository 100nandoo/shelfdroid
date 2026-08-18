# 02 — Edit the login message and Login methods

**What to build:** Turn the read-only overview into a saveable Authentication form for the custom
login message, Password sign-in, and OpenID login. Admins can preview server HTML, reset drafts,
confirm dangerous changes, and save a changed-fields-only update without leaving the server with
no usable Login method.

**Blocked by:** 01 — Admin-only Authentication settings overview.

**Status:** ready-for-agent

- [ ] The custom login message can be enabled, edited as HTML source, rendered with the same
      semantics as Login, disabled, and reset to the last canonical server snapshot.
- [ ] Existing complex HTML remains unchanged when the admin does not edit the message.
- [ ] Disabling or blanking the custom message submits an explicit clear rather than omitting the
      field.
- [ ] Password sign-in and OpenID login switches reflect and update only the supported `local` and
      `openid` Login methods.
- [ ] Incomplete OpenID configuration can be saved while OpenID remains disabled.
- [ ] The form rejects a state with no active Login method.
- [ ] Disabling Password sign-in requires confirmation and is blocked unless enabled OpenID
      configuration is structurally valid.
- [ ] Save is enabled only when the draft is valid, dirty, and not already saving; Reset restores
      the complete saved snapshot.
- [ ] Back warns before discarding unsaved changes and clears transient operation state when the
      admin confirms leaving.
- [ ] Saving sends only changed fields, preserves untouched server values, and uses the dedicated
      partial-update endpoint.
- [ ] A non-empty update that returns `updated: false` is surfaced as rejected or skipped rather
      than successful.
- [ ] An accepted update reloads canonical Authentication settings and establishes a clean saved
      snapshot.
- [ ] Repository and ViewModel tests cover load, edit, clear, reset, validation, confirmation,
      no-op, partial Save, skipped update, canonical reload, and unsaved Back behavior.
- [ ] Integration coverage confirms Login discovery observes the saved custom message and active
      Login methods after the server applies them.
