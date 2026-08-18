# 03 — Configure and discover the OpenID provider

**What to build:** Let an admin configure the OpenID issuer, provider endpoints, client ID, and
signing algorithm, either manually or by asking the Audiobookshelf server to discover provider
metadata. The completed slice validates active OpenID configuration without requiring a live test
login and explains the server restart requirement after saving.

**Blocked by:** 02 — Edit the login message and Login methods.

**Status:** ready-for-agent

- [ ] The form supports issuer, authorization, token, userinfo, JWKS, logout, client ID, and signing
      algorithm values without exposing or editing the client secret in this ticket.
- [ ] Required OpenID fields are validated before OpenID can be enabled or Password sign-in can be
      disabled.
- [ ] Auto-populate calls the authenticated Audiobookshelf issuer-discovery endpoint rather than
      contacting the identity provider directly.
- [ ] Discovery works for servers installed at the URL root and under a subpath.
- [ ] Successful discovery updates provider-owned endpoints and signing algorithm choices while
      preserving client data, callback settings, User-mapping edits, and unrelated draft fields.
- [ ] The current signing algorithm is preserved until discovery succeeds and remains selectable
      when supported by the discovered provider.
- [ ] Discovery failure preserves the complete draft and identifies discovery as the failed
      operation.
- [ ] Manual edits and discovered values participate in existing dirty-state, Reset, partial Save,
      skipped-update, canonical-reload, and unsaved Back behavior.
- [ ] Successful saves that change OIDC configuration show a persistent message that the
      Audiobookshelf server must be restarted before all changes take effect.
- [ ] Discovery and Save results cannot overwrite admin edits made after those operations began.
- [ ] Repository tests cover root and subpath discovery URLs, successful merge behavior, failure
      preservation, algorithm selection, required-field validation, and changed-fields-only Save.
- [ ] UI and ViewModel coverage verifies manual editing, discovery loading, discovery failure,
      provider result application, validation, operation serialization, and restart messaging.
