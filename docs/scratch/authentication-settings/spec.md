# Authentication Settings

**Triage:** `ready-for-agent`

## Problem Statement

ShelfDroid administrators cannot manage the **Authentication settings** of their Audiobookshelf
server from the Android app. They must leave ShelfDroid and use the Audiobookshelf web client to
configure login-facing messaging, **Password sign-in**, **OpenID login**, identity-provider
endpoints, mobile callback allowlists, and OpenID User mapping.

This server-wide configuration is security-sensitive and can lock every **User** out when changed
incorrectly. The Audiobookshelf server also returns its raw OpenID client secret from the dedicated
admin endpoint. ShelfDroid therefore needs a deliberate admin/root-only experience that validates
dangerous changes, preserves untouched values, never retains or redisplays the loaded secret, and
explains when an Audiobookshelf server restart is required.

## Solution

Add a dedicated **Authentication** screen to ShelfDroid's admin-only Server section. It will let
admin and root Users inspect and update the server's **Authentication settings** while keeping User
account management and broader **Server settings** separate.

The screen will provide an HTML login-message editor with rendered preview, Password sign-in and
OpenID login controls, OIDC issuer discovery, provider and client fields, mobile redirect URI
management, and OpenID User-mapping options. It will prevent configurations with no usable
**Login method**, require confirmation for lockout-prone changes, submit only changed fields, and
reload canonical values after saving.

## User Stories

1. As an Audiobookshelf admin, I want to find Authentication in ShelfDroid's Server section, so
   that I can manage server sign-in without opening the web client.
2. As an Audiobookshelf root User, I want the same Authentication access as an admin, so that I can
   manage the server I own.
3. As a regular User, I want Authentication to be absent from my navigation, so that I am not
   offered server security controls I cannot use.
4. As a guest User, I want Authentication to be absent from my navigation, so that sensitive
   server configuration remains private.
5. As an admin whose role was revoked, I want the next Authentication request denied, so that stale
   local state does not expose server configuration.
6. As an admin, I want a clear access-denied state when the server returns `403`, so that I
   understand why the screen is no longer available.
7. As a security-conscious admin, I want a locally known non-admin destination load to make no
   Authentication API calls, so that secret-bearing configuration is not requested unnecessarily.
8. As an admin, I want to see the server's current Login methods before editing them, so that I
   understand how Users currently sign in.
9. As an admin, I want to enable Password sign-in, so that Users can authenticate with a username
   and password.
10. As an admin, I want to disable Password sign-in only after confirmation, so that I do not
    accidentally remove the safest recovery path.
11. As an admin, I want ShelfDroid to reject a state with no active Login method, so that the server
    cannot be saved without a sign-in path.
12. As an admin, I want disabling Password sign-in blocked until enabled OpenID configuration is
    structurally valid, so that I do not create an immediate lockout.
13. As an admin, I want to enable OpenID login, so that Users can authenticate through the server's
    identity provider.
14. As an admin, I want to disable OpenID login, so that a broken or retired provider is no longer
    offered.
15. As an admin, I want to draft incomplete OpenID configuration while OpenID remains disabled, so
    that I can prepare a provider safely over multiple edits.
16. As an admin, I want required OpenID fields identified before enabling it, so that I can finish
    a valid configuration.
17. As an admin, I want to enter an issuer URL and ask Audiobookshelf to discover provider metadata,
    so that I do not have to copy every endpoint manually.
18. As an admin, I want issuer discovery failures to preserve my complete draft, so that a provider
    outage does not destroy manual edits.
19. As an admin, I want discovered authorization, token, userinfo, logout, and JWKS endpoints
    populated, so that provider configuration is consistent.
20. As an admin, I want discovered signing algorithms offered as choices, so that I can select an
    algorithm supported by the provider.
21. As an admin, I want the current signing algorithm preserved until discovery succeeds, so that
    discovery failure does not alter working configuration.
22. As an admin using a server installed under a URL subpath, I want issuer discovery to preserve
    the subpath, so that the request reaches my Audiobookshelf server.
23. As an admin, I want to edit provider endpoints after discovery, so that I can apply intentional
    overrides without rerunning discovery.
24. As an admin, I want to edit the OpenID client ID, so that Audiobookshelf identifies itself
    correctly to the provider.
25. As an admin, I want to know whether a client secret is configured without seeing its raw value,
    so that I can assess configuration safely.
26. As an admin, I want to enter a replacement client secret in a masked field, so that I can rotate
    provider credentials.
27. As an admin, I want to reveal replacement text temporarily, so that I can verify it before
    saving.
28. As an admin, I want an untouched client secret omitted from updates, so that saving unrelated
    changes cannot overwrite it.
29. As an admin, I want clearing the client secret to require explicit confirmation, so that I do
    not break OpenID login accidentally.
30. As a security-conscious admin, I want replacement secret text discarded after completion,
    failure, reset, access denial, or navigation away, so that ShelfDroid does not retain it.
31. As an admin, I want to manage allowed mobile redirect URIs as a list, so that authorized mobile
    clients can finish OpenID login.
32. As a ShelfDroid admin, I want a warning and confirmation before removing
    `audiobookshelf://oauth`, so that I understand ShelfDroid OpenID login will stop working.
33. As an admin, I want invalid mobile redirect URIs rejected before saving, so that silently
    skipped server updates do not appear successful.
34. As an admin, I want wildcard redirect allowed only as the sole entry and after high-risk
    confirmation, so that broad callback access is always deliberate.
35. As an admin, I want the loaded callback subfolder preserved, so that OpenID callbacks continue
    to work for subpath installations.
36. As an admin, I want to choose only no callback subfolder or the server-provided base path, so
    that I cannot invent an unusable callback route.
37. As an admin, I want to see effective web and mobile callback details, so that I can configure
    the identity provider correctly.
38. As an admin, I want to customize the OpenID button text, so that Users recognize the identity
    provider on ShelfDroid's Login screen.
39. As an admin, I want to match existing Users by email, username, or not at all, so that account
    linking follows my server policy.
40. As an admin, I want unsupported existing-User matching values blocked, so that the server does
    not silently behave as though matching were disabled.
41. As an admin, I want to enable automatic OpenID launch, so that Users go directly to the identity
    provider from Login.
42. As an admin, I want to disable automatic OpenID launch, so that Users can choose another Login
    method when more than one is available.
43. As an admin, I want to enable automatic User registration, so that unmatched provider Users can
    be created without manual setup.
44. As an admin, I want to disable automatic User registration, so that only pre-provisioned Users
    can access the server.
45. As an admin, I want to configure the OpenID group claim, so that provider groups can map Users
    to admin, user, or guest roles.
46. As an admin, I want to configure the advanced permissions claim, so that non-admin permissions
    can be supplied by the identity provider.
47. As an admin, I want to see the server's sample advanced-permissions JSON, so that I can configure
    the provider claim in the expected shape.
48. As an admin, I want to enable or disable a custom login message, so that Users receive relevant
    instructions before signing in.
49. As an admin, I want to edit the custom login message as HTML and see a rendered preview, so that
    existing formatted content round-trips without a new editor dependency.
50. As an admin, I want disabling or clearing the custom message to remove it from Login discovery,
    so that stale instructions are not shown.
51. As an admin, I want every field and toggle to have a visible label and TalkBack semantics, so
    that Authentication is accessible.
52. As an admin, I want Save disabled when there are no changes, so that I do not send meaningless
    updates.
53. As an admin, I want Save disabled while validation fails, so that invalid values are not
    silently skipped by the server.
54. As an admin, I want Save disabled while another save is running, so that concurrent updates do
    not race.
55. As an admin, I want issuer discovery and save results serialized against my edits, so that stale
    asynchronous results cannot overwrite newer input.
56. As an admin, I want Reset to restore the last canonical server snapshot, so that I can abandon
    unwanted edits.
57. As an admin, I want Back to warn when unsaved changes exist, so that I do not discard a long
    configuration draft accidentally.
58. As an admin, I want only changed settings submitted, so that untouched Authentication settings
    and credentials are preserved.
59. As an admin, I want `updated: false` for a non-empty update treated as rejected or skipped, so
    that HTTP success does not mislead me.
60. As an admin, I want ShelfDroid to reload Authentication settings after an accepted update, so
    that the screen reflects canonical server values.
61. As an admin, I want operation-specific errors for load, discovery, and save, so that I know
    which action needs attention.
62. As an admin, I want a clear success message after saving, so that I know the server accepted the
    change.
63. As an admin, I want a persistent warning that changed OIDC configuration requires an
    Audiobookshelf server restart, so that I understand delayed behavior.
64. As a currently signed-in admin, I want saving Authentication settings to preserve my current
    ShelfDroid session, so that I can finish administration safely.
65. As a User returning to Login after settings change and server restart, I want Login discovery to
    honor the new message, Login methods, button text, and auto-launch setting, so that ShelfDroid
    matches current server policy.

## Implementation Decisions

- Authentication will be a dedicated admin/root screen in the existing `Misc` Server group.
- Authentication settings, broader Server settings, and User management remain separate domain and
  API boundaries.
- Administrative access maps to Audiobookshelf `admin` or `root`. Individual content permissions do
  not grant access.
- A reusable admin-destination guard will be introduced and used by Authentication. Migrating other
  existing admin destinations is separate work.
- Navigation visibility is not authorization. Local User state will be checked before loading, and
  the Audiobookshelf server's `403` remains authoritative when local state is stale.
- The feature will use authenticated `GET /api/auth-settings` and `PATCH /api/auth-settings`; it
  will not reuse the broader Server settings endpoint.
- Issuer discovery will use authenticated `GET /auth/openid/config?issuer=...` through the
  Audiobookshelf server rather than contacting the identity provider directly.
- Endpoint resolution will preserve the configured Audiobookshelf server base path.
- Network request and response models will be dedicated to Authentication settings rather than
  expanding general Login or Server settings models.
- Update fields will be optional and only changed fields will be submitted. Omitted fields preserve
  their current server value.
- ShelfDroid's serializer omits nulls. Explicit string clears will use empty strings, which the
  server canonicalizes to null, except that an empty callback subfolder intentionally means no
  subfolder.
- Active Login methods will contain only `local`, `openid`, or both; an empty active set is invalid.
- Incomplete OIDC settings may be saved while OpenID remains disabled, but OpenID enablement and
  Password sign-in disablement require structurally valid OIDC configuration.
- Password sign-in disablement requires explicit confirmation. A live test login is not required.
- The client will validate known field contracts because the server usually skips invalid values
  while returning HTTP `200`.
- A non-empty patch that returns `updated: false` will be surfaced as a rejected or skipped update.
- An accepted update will be followed by a fresh Authentication settings read because the PATCH
  response does not contain the complete Authentication configuration.
- Form state will use canonical saved and editable draft snapshots for dirty checking, reset,
  unsaved-change confirmation, and changed-field request construction.
- Load, issuer discovery, and save will have distinct operation states. Discovery and save results
  will not overwrite edits made after an operation began.
- The loaded client secret will be reduced immediately to configured/not-configured and will never
  enter form state, persistence, navigation state, previews, logs, or analytics.
- Secret intent has three states: untouched, replace, and explicit clear. Untouched omits the field,
  replacement sends entered text once, and clear sends an empty string after confirmation.
- Secret replacement text will exist only in ViewModel memory, use masked input with temporary
  reveal, and be cleared on completion, failure, reset, access denial, and navigation away.
- Custom login message enablement is represented by a non-empty message. Disabled is encoded as an
  explicit clear because the server has no separate enable flag.
- The initial message editor will use HTML source plus the same rendered-preview semantics as
  ShelfDroid's Login screen. A WYSIWYG dependency will not be added.
- Discovery will update provider-owned endpoints and signing algorithm choices while preserving
  client credentials, callbacks, User-mapping edits, and unrelated draft fields.
- The current signing algorithm will remain selected until successful discovery supplies supported
  alternatives.
- Mobile redirect validation will match the server contract. Wildcard requires exclusivity and
  high-risk confirmation.
- Removing `audiobookshelf://oauth` is allowed only after warning and confirmation.
- Callback subfolder choices are restricted to no subfolder or the server-provided base path.
- Existing-User matching is restricted to none, email, or username.
- Button text, auto-launch, auto-register, group claim, advanced-permissions claim, and read-only
  sample permissions are included in the first feature.
- Saving will not force logout, restart ShelfDroid, rerun Login discovery for the current session,
  or restart the Audiobookshelf server.
- Successful OIDC changes will state that the Audiobookshelf server must be restarted before all
  changes take effect.
- Screen-capture and recent-app-preview protection are explicitly deferred.
- The domain glossary defines Authentication settings separately from Login methods, Login
  discovery, User management, Server settings, and Local app preferences.
- The accepted ADR records the dedicated screen and endpoint boundary, admin/root access, partial
  updates, and loaded-secret reduction policy.

## Testing Decisions

- Tests will assert externally visible behavior and serialized contracts rather than private
  helpers, exact internal state mutations, or Compose layout structure.
- The primary test seam is the authenticated Authentication settings repository with controlled
  current-User state and an HTTP test server. This highest existing seam verifies authorization,
  endpoint resolution, request bodies, response mapping, secret reduction, discovery merging,
  save outcomes, and canonical reload together.
- The primary seam will verify that non-admin load, discovery, and save perform zero HTTP requests.
- The primary seam will verify that server `403` produces access denial and discards settings and
  transient sensitive input.
- The primary seam will verify root and subpath URLs for read, update, and discovery.
- The primary seam will verify no-op behavior, changed-fields-only PATCH, explicit clears,
  `updated: false`, successful reload, and operation-specific failures.
- The primary seam will verify that raw secret presence becomes configured/not-configured and that
  untouched, replacement, and clear intents produce the correct external request behavior.
- Focused network serialization tests are justified for the omit-versus-empty-string contract under
  the shared null-omitting serializer.
- Focused validation tests will cover Login method lockout, required OIDC fields, redirect URI
  syntax, wildcard exclusivity, callback subfolder choices, existing-User matching values,
  custom-message clearing, and the ShelfDroid callback warning.
- Focused ViewModel tests will cover operation serialization, unsaved-change state, reset, secret
  cleanup, discovery result application, and save prevention when clean, invalid, or busy.
- Focused UI tests will cover admin-only entry visibility, access denial, confirmation flows,
  secret masking, restart messaging, field labels, keyboard behavior, and TalkBack semantics.
- UI tests will avoid pixel-position and full-hierarchy assertions. Previews may cover meaningful
  loading, ready, failure, access-denied, invalid, discovery, and saving states.
- Integration verification will confirm that existing Login discovery observes newly saved Login
  methods, custom message, OpenID button text, and auto-launch after server application.
- Manual verification will cover admin, root, user, guest, role revocation, root installation,
  subpath installation, Password-only, OpenID-only, both Login methods, secret replacement, secret
  clearing, wildcard redirect, and removal of the ShelfDroid callback URI.
- Prior art includes existing Email settings saved/draft repository tests, Apprise notification
  settings mapping and validation tests, Login discovery repository tests, Login ViewModel state
  tests, and navigation result tests.

## Out of Scope

- Creating, editing, disabling, deleting, or unlinking individual Users.
- Replacing ShelfDroid's existing User management screens.
- Managing API keys, access tokens, refresh tokens, or bearer sessions.
- Configuring an identity provider outside the Audiobookshelf server.
- Requiring or performing a live administrator OpenID login during Save.
- Restarting the Audiobookshelf server.
- Automatically logging out or forcing re-login after updating Authentication settings.
- Changing ShelfDroid's `audiobookshelf://oauth` callback scheme.
- Adding a WYSIWYG rich-text dependency in the initial implementation.
- Migrating every existing admin destination to the reusable guard.
- Screen-capture and recent-app-preview protection.

## Further Notes

The browser inspection, Audiobookshelf API research, ShelfDroid architecture findings, security
analysis, and detailed implementation guidance are recorded in the related
[Authentication Settings Findings and Implementation Plan](../../plan/authentication-management.md).

The dedicated-screen and boundary decision is recorded in
[ADR 0013: Dedicated Admin Authentication Settings Screen](../../adr/0013-dedicated-admin-authentication-settings-screen.md).

Audiobookshelf derives administrative access from User type (`admin` or `root`), not granular
content permissions. The dedicated Authentication settings read endpoint returns the raw OpenID
client secret even though normal browser-safe Server settings omit it. Immediate secret reduction
and destination-level access checks are therefore required behavior, not optional hardening.
