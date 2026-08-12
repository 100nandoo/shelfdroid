# Tickets: Data-layer session reset boundary

These tickets move **Full logout** and **Account switch** orchestration out of the settings repository and into a data-layer session reset boundary. Source spec: [Spec: Data-layer session reset boundary](../data-layer-session-reset-boundary.md).

Work the **frontier**: any ticket whose blockers are all done. For this work, start with the session reset boundary, then local cleanup collaborators, then UI wiring, then contract and verification.

## Create the session reset boundary

**What to build:** Add the data-layer owner for **Full logout** and **Account switch**, with behavior covered by focused tests, while existing UI behavior can still be wired later. This establishes the public orchestration API and verifies remote logout policy: full logout only cleans locally after successful remote logout; account switch can clean locally even without a refresh token.

**Blocked by:** None — can start immediately.

- [x] A data-layer session reset boundary exposes full logout behavior.
- [x] A data-layer session reset boundary exposes account-switch behavior.
- [x] Full logout returns failure and does not run local cleanup when remote logout fails.
- [x] Full logout returns failure and does not run local cleanup when the current session has no refresh token.
- [x] Full logout runs local cleanup only after successful remote logout.
- [x] Account switch attempts remote logout when a refresh token exists.
- [x] Account switch still runs local cleanup when no refresh token exists.
- [x] Existing settings and login callers can continue compiling until they are migrated.

## Extract local database and app storage cleanup

**What to build:** Move destructive SQLDelight table cleanup and Android cache deletion behind data-layer cleanup collaborators used by the session reset boundary. This makes database/cache reset behavior testable outside the settings repository and keeps completed **Downloads** out of **Cached content** cleanup.

**Blocked by:** Create the session reset boundary.

- [x] Local database cleanup is owned by a dedicated data-layer collaborator.
- [x] App storage cleanup is owned by a dedicated data-layer collaborator.
- [x] The session reset boundary uses the cleanup collaborators rather than owning SQLDelight table deletion or Android cache deletion directly.
- [x] Database cleanup preserves the required deletion transaction behavior.
- [x] App storage cleanup deletes app cache locations without adding Android context dependencies to the settings repository.
- [x] Completed **Downloads** are not deleted as part of **Cached content** cleanup.

## Add factory-reset local app preference cleanup

**What to build:** Make **Full logout** and **Account switch** reset **Local app preferences** through the session reset boundary, matching the clarified product decision that full logout means local preference factory reset.

**Blocked by:** Create the session reset boundary.

- [x] Local app preference reset is explicit in the session reset boundary's local cleanup path.
- [x] Full logout resets local app preferences after successful remote logout.
- [x] Account switch resets local app preferences as part of local cleanup.
- [x] Forced re-login behavior remains unchanged and does not use the destructive local preference reset path.
- [x] Tests cover local app preference reset as externally visible cleanup behavior.

## Wire Settings full logout through the session reset boundary

**What to build:** The settings-screen **Full logout** action no longer depends on settings repository cleanup. It keeps the same visible behavior, including surfacing remote logout failure without destructive local cleanup.

**Blocked by:** Create the session reset boundary; Extract local database and app storage cleanup; Add factory-reset local app preference cleanup.

- [x] The settings-screen full logout flow calls the session reset boundary.
- [x] Remote logout failure is still surfaced to the settings-screen user.
- [x] Remote logout failure still prevents destructive local cleanup.
- [x] Successful full logout still routes the user away from the current local session as before.
- [x] Settings preference reads and updates remain available to settings UI.

## Wire login Account switch through the session reset boundary

**What to build:** The login-flow **Account switch** path no longer depends on the settings repository and still clears local state before returning the user to login for a different **User** or **Audiobookshelf server**.

**Blocked by:** Create the session reset boundary; Extract local database and app storage cleanup; Add factory-reset local app preference cleanup.

- [x] The login-flow account-switch path calls the session reset boundary.
- [x] Account switch clears local state when the current session has a refresh token.
- [x] Account switch clears local state when the refresh token is already gone.
- [x] Account switch still returns the user to the login flow for another **User** or **Audiobookshelf server**.
- [x] Login behavior unrelated to account switching remains unchanged.

## Contract SettingsRepository back to settings-only

**What to build:** Remove logout orchestration, database cleanup, storage cleanup, remote logout, and unrelated dependencies from the settings repository so it owns only local app preference reads and updates.

**Blocked by:** Wire Settings full logout through the session reset boundary; Wire login Account switch through the session reset boundary.

- [ ] The settings repository no longer exposes full logout behavior.
- [ ] The settings repository no longer exposes account-switch behavior.
- [ ] The settings repository no longer depends on remote logout, database cleanup, app storage cleanup, or download cleanup dependencies.
- [ ] The settings repository continues to expose local app preference streams needed by current callers.
- [ ] The settings repository continues to update theme, list presentation, filter, sort order, and related local app preferences.
- [ ] No UI caller depends on settings repository for session reset behavior.

## Verify full session reset behavior end to end

**What to build:** Run the relevant build/tests and close any integration gaps around full logout, account switch, current playback cleanup representation, and dependency injection wiring.

**Blocked by:** Contract SettingsRepository back to settings-only.

- [ ] Relevant unit tests for the session reset boundary pass.
- [ ] Relevant UI or view-model tests for settings full logout and login account switch pass if present or added.
- [ ] The relevant Android/Kotlin build target compiles.
- [ ] Dependency injection wiring resolves for the new data-layer collaborators.
- [ ] Current playback cleanup is represented in the session reset path or explicitly documented as a remaining follow-up if no reset collaborator exists yet.
- [ ] The final implementation still respects the accepted ADRs for forced re-login, full logout, and the data-layer session boundary.
