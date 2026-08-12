---
title: Spec: Data-layer session reset boundary
labels:
  - ready-for-agent
related:
  - ../adr/0004-session-recovery-forced-relogin-and-full-logout.md
  - ../adr/0009-data-layer-session-boundary-before-domain-layer.md
---

# Spec: Data-layer session reset boundary

## Problem Statement

ShelfDroid currently routes **Full logout** and **Account switch** cleanup through the settings data path. That makes the settings repository responsible for remote logout, local authentication cleanup, cached catalog and playback data cleanup, transient download state cleanup, database table deletion, cache directory deletion, and local app preference reset. From a maintainer perspective, this hides destructive session-reset behavior inside a settings component and makes it unclear which data-layer boundary owns local reset policy.

## Solution

ShelfDroid will move **Full logout** and **Account switch** orchestration out of the settings repository into a data-layer session reset boundary. The settings repository will remain responsible for local app preference reads and writes, while the new session reset boundary coordinates authentication, local app preference reset, cached content cleanup, transient download state cleanup, current playback cleanup, local database cleanup, and app storage cleanup.

This change will not introduce a domain layer. It will use data-layer repositories and data sources, in accordance with the accepted architecture decision to keep session reset orchestration in the data layer until a broader domain layer is justified.

## User Stories

1. As a ShelfDroid maintainer, I want **Full logout** cleanup to live outside the settings repository, so that settings code only owns settings behavior.
2. As a ShelfDroid maintainer, I want **Account switch** cleanup to live outside the settings repository, so that login-flow transitions do not depend on a screen-specific settings repository.
3. As a ShelfDroid maintainer, I want one data-layer session reset boundary, so that destructive local cleanup has a clear owner.
4. As a ShelfDroid maintainer, I want **Full logout** and **Account switch** to share local cleanup behavior, so that both flows reset the same local state consistently.
5. As a ShelfDroid maintainer, I want remote logout policy to remain explicit, so that **Full logout** still cancels destructive cleanup when the Audiobookshelf server rejects remote logout.
6. As a ShelfDroid maintainer, I want **Account switch** to tolerate a missing refresh token, so that an intentional login-flow transition can still clean local state when remote logout is no longer possible.
7. As a ShelfDroid user, I want **Full logout** to clear local authentication state, so that the next person opening ShelfDroid cannot use my session.
8. As a ShelfDroid user, I want **Full logout** to clear cached catalog and playback data, so that server-derived local data from the previous session is removed.
9. As a ShelfDroid user, I want **Full logout** to clear transient download state, so that abandoned in-progress download state does not leak into the next session.
10. As a ShelfDroid user, I want **Full logout** to clear current playback, so that the next session does not resume the previous user's local playback context.
11. As a ShelfDroid user, I want **Full logout** to reset local app preferences, so that the app returns to a factory-reset local preference state.
12. As a ShelfDroid user, I want **Full logout** not to be confused with **Forced re-login**, so that session recovery can preserve local data while explicit logout remains destructive.
13. As a ShelfDroid user switching **User**, I want **Account switch** to clear local session state, so that the next user starts from a clean local app state.
14. As a ShelfDroid user switching **Audiobookshelf server**, I want **Account switch** to clear server-derived local data, so that data from one server does not appear while using another server.
15. As a ShelfDroid user with completed **Downloads**, I want completed downloads to be treated separately from cached content, so that maintainers can make deletion policy explicit rather than accidentally deleting offline media through cache cleanup.
16. As a settings-screen user, I want theme, list presentation, filters, and sort order controls to continue working, so that moving logout orchestration does not regress ordinary settings behavior.
17. As a login-flow user, I want the existing account-switch path to keep routing me to the login screen after cleanup, so that the user experience remains unchanged.
18. As a settings-screen user, I want the existing full-logout action to keep surfacing remote logout errors, so that failed remote logout does not silently erase local state.
19. As a ShelfDroid maintainer, I want database cleanup to be owned by a local database abstraction, so that table deletion order and transactions are not duplicated in screen repositories.
20. As a ShelfDroid maintainer, I want app cache cleanup to be owned by a storage abstraction, so that Android context usage is isolated from settings behavior.
21. As a ShelfDroid maintainer, I want local app preference reset to be explicit in the reset boundary, so that factory-reset semantics are visible in code and tests.
22. As a ShelfDroid maintainer, I want destructive cleanup order to be testable at the session reset boundary, so that future changes do not accidentally skip part of **Full logout**.
23. As a ShelfDroid maintainer, I want current playback cleanup to be represented in the reset boundary, so that ADR-defined logout behavior is fully implemented rather than only clearing persisted data.
24. As a ShelfDroid maintainer, I want the existing playback/listening session repositories to remain focused on listening sessions, so that “session reset” does not blur with **Listening session** synchronization.

## Implementation Decisions

- Do not introduce a domain layer for this change.
- Move **Full logout** and **Account switch** orchestration to a data-layer session reset boundary.
- Keep the settings repository focused on local app preference streams and preference update methods.
- Treat **Full logout** as a factory reset of local app preferences in addition to clearing local authentication state, cached catalog and playback data, transient download state, and current playback.
- Treat **Account switch** as an intentional login-flow transition where the user leaves the current **User** or **Audiobookshelf server** and signs in as a different user or server.
- Preserve the existing **Full logout** policy from the accepted ADR: destructive local cleanup only happens after the Audiobookshelf server accepts the remote logout request.
- Preserve the existing **Account switch** escape-hatch policy from the accepted ADR: destructive local cleanup may still run when remote logout can no longer succeed because the refresh token is already gone.
- Introduce or reuse a data-layer session reset repository as the single public orchestration boundary for `fullLogout` and `logoutForAccountSwitch` behavior.
- Introduce a local cleanup collaborator owned by the data layer to coordinate local authentication reset, local app preference reset, cached content cleanup, transient download cleanup, local database cleanup, app storage cleanup, and current playback cleanup.
- Put SQLDelight table deletion order and transaction ownership behind a local database cleanup abstraction.
- Put Android cache directory deletion behind an app storage cleanup abstraction.
- Keep completed **Downloads** distinct from **Cached content**. This spec does not define deletion of completed downloads as part of full logout.
- Avoid naming that implies generic Audiobookshelf **Listening session** behavior for the reset boundary. Existing listening-session repositories should stay focused on playback session synchronization.
- Update UI callers so settings-triggered **Full logout** and login-triggered **Account switch** call the session reset boundary instead of the settings repository.
- Keep dependency injection constructor-based and data-layer scoped.
- Do not change remote API contracts.
- Do not change database schema.
- Do not change product copy or navigation behavior except where required to route callers through the new boundary.

## Testing Decisions

- Good tests should assert externally visible behavior at the session reset boundary, not private helper methods or implementation-specific table names.
- The primary test seam is the data-layer session reset repository. Tests should call `fullLogout` and `logoutForAccountSwitch` and assert collaborator effects and returned results.
- The settings repository should have narrow tests only for retained settings behavior if test infrastructure is added there; it should not be the main logout cleanup test seam.
- **Full logout** tests should verify that local cleanup is not run when remote logout fails.
- **Full logout** tests should verify that local cleanup runs after remote logout succeeds.
- **Full logout** tests should verify that missing refresh token fails the operation and does not run destructive cleanup.
- **Account switch** tests should verify that a present refresh token triggers a remote logout attempt and then runs local cleanup.
- **Account switch** tests should verify that a missing refresh token still runs local cleanup.
- Local cleanup tests should verify that local app preferences are reset as part of factory-reset semantics.
- Local cleanup tests should verify that transient download state cleanup is requested.
- Local cleanup tests should verify that cached catalog and playback database cleanup is requested through the local database cleanup abstraction.
- Local cleanup tests should verify that app storage cleanup is requested through the app storage cleanup abstraction.
- Local cleanup tests should verify that current playback cleanup is represented once the current playback reset collaborator exists.
- Database cleanup tests may assert behavior using the highest available database abstraction and should avoid coupling to unrelated repository internals.
- UI-layer tests, if added, should only assert that settings and login flows call the session reset boundary and react to success or failure. They should not duplicate data-layer cleanup assertions.
- There is no established unit test suite in the usual test source sets, so this work should add focused tests around the new data-layer boundary rather than trying to retrofit broad coverage across the app.

## Out of Scope

- Introducing a domain layer or use cases.
- Redesigning the broader authentication architecture.
- Changing **Forced re-login** behavior.
- Changing login screen navigation or product copy.
- Changing remote logout API behavior.
- Changing database schema.
- Deleting completed **Downloads** as part of full logout.
- Reworking existing playback **Listening session** synchronization repositories.
- Refactoring unrelated screen repositories.
- Adding a comprehensive test framework migration beyond the focused tests needed for this boundary.

## Further Notes

- This spec follows the glossary definitions for **Account switch**, **Full logout**, **Local app preferences**, and **Cached content**.
- This spec respects the accepted decision that **Full logout** and **Forced re-login** are different policies: forced re-login preserves local app data and cached content, while full logout is destructive and resets local app preferences.
- This spec also respects the accepted decision to create domain-oriented data-layer seams while avoiding premature domain-layer introduction.
- The current code already has session-related data-layer classes for playback/listening sessions. The implementation should avoid overloading those concepts; “session reset” here means authentication and local app reset, not server **Listening session** synchronization.
