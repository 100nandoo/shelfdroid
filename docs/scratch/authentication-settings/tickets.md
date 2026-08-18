# Authentication Settings Tickets

**Parent spec:** [Authentication Settings](spec.md)

| Ticket | Title | Blocked by | Status |
| --- | --- | --- | --- |
| [01](issues/01-admin-only-authentication-settings-overview.md) | Admin-only Authentication settings overview | None | `ready-for-human` |
| [02](issues/02-login-message-and-methods.md) | Edit the login message and Login methods | 01 | `ready-for-human` |
| [03](issues/03-openid-provider-discovery.md) | Configure and discover the OpenID provider | 02 | `ready-for-human` |
| [04](issues/04-openid-client-secret.md) | Rotate the OpenID client secret securely | 03 | `ready-for-human` |
| [05](issues/05-mobile-openid-callbacks.md) | Manage mobile OpenID callbacks | 03 | `ready-for-human` |
| [06](issues/06-openid-user-mapping.md) | Configure OpenID User mapping and registration | 03 | `ready-for-agent` |
| [07](issues/07-hardening-and-verification.md) | Harden and verify Authentication settings | 04, 05, 06 | `ready-for-agent` |

Tickets 01, 02, and 03 are ready for human verification. Tickets 04, 05, and 06 can proceed in
parallel after ticket 03 is complete.
