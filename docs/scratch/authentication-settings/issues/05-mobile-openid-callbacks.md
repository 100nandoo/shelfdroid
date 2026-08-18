# 05 — Manage mobile OpenID callbacks

**What to build:** Let admins manage the mobile redirect URI allowlist and callback subfolder while
making the consequences for ShelfDroid's OpenID login explicit. The completed slice supports both
root and subpath Audiobookshelf installations and prevents invalid or accidental broad callback
updates.

**Blocked by:** 03 — Configure and discover the OpenID provider.

**Status:** ready-for-agent

- [ ] Admins can add, edit, and remove mobile redirect URIs through a list-oriented control.
- [ ] Each redirect URI is validated against the Audiobookshelf server's accepted mobile URI
      contract before Save.
- [ ] Wildcard redirect is accepted only when it is the sole allowlist entry and after high-risk
      confirmation.
- [ ] Removing `audiobookshelf://oauth` requires warning and confirmation that ShelfDroid OpenID
      login will stop working.
- [ ] The callback subfolder preserves the loaded value and offers only no subfolder or the
      server-provided base-path option.
- [ ] Empty callback subfolder is serialized as an intentional empty string rather than omitted or
      null.
- [ ] The screen shows effective web and mobile callback details needed for identity-provider
      configuration without exposing unrelated secrets.
- [ ] Redirect and subfolder edits participate in dirty-state, Reset, unsaved Back, partial Save,
      skipped-update, canonical-reload, and restart-warning behavior.
- [ ] Repository tests cover valid URIs, invalid URIs, wildcard exclusivity and confirmation,
      explicit empty subfolder, root installation, subpath installation, and changed-field updates.
- [ ] UI and ViewModel tests cover list editing, validation, callback warning, confirmations,
      subfolder choices, calculated callback details, Reset, and accepted Save.
