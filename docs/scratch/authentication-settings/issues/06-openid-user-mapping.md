# 06 — Configure OpenID User mapping and registration

**What to build:** Let admins control how OpenID login is presented and how authenticated provider
identities are matched, registered, assigned roles, and assigned non-admin permissions. The slice
constrains values to behavior the Audiobookshelf server actually understands.

**Blocked by:** 03 — Configure and discover the OpenID provider.

**Status:** ready-for-human

- [x] Admins can edit the OpenID login button text and Reset it to the last server snapshot.
- [x] Existing-User matching offers exactly none, email, and username.
- [x] Auto-launch and auto-register controls load, edit, reset, validate, and save as real booleans.
- [x] Admins can edit the optional group claim used for admin, user, or guest role mapping.
- [x] Admins can edit the optional advanced permissions claim used for non-admin permission data.
- [x] The server-provided sample advanced-permissions JSON is displayed read-only and is never
      submitted as an editable setting.
- [x] Claim inputs follow the same validation rules as the Audiobookshelf reference client and do
      not submit accidental whitespace-only values.
- [x] Mapping and registration edits participate in dirty-state, Reset, unsaved Back, partial Save,
      skipped-update, canonical-reload, and restart-warning behavior.
- [x] Repository tests cover supported matching values, explicit clearing, booleans, claim
      validation, read-only sample mapping, and changed-field updates.
- [x] UI and ViewModel tests cover every control, validation, Reset, operation feedback, and
      read-only sample presentation.
- [x] Integration coverage confirms saved button text and auto-launch are observed by ShelfDroid's
      existing Login discovery.
