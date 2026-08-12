# Tickets: Login-time server access mode

A tracer-bullet breakdown for the login-time local-network server access fix. Source spec: [../login-time-server-access-mode.md](../login-time-server-access-mode.md).

Work the **frontier**: any ticket whose blockers are all done. For this chain, work top to bottom.

## Add explicit server access mode to LoginScreen

**What to build:** Users can choose how ShelfDroid should reach the current **Audiobookshelf server** during login by selecting `Internet` or `Local network` on `LoginScreen`. The choice defaults to `Internet`, is persisted with the current server, and is restored during future sign-in attempts. During **Forced re-login**, ShelfDroid reuses the saved server access mode and does not allow changing it.

**Blocked by:** None — can start immediately.

- [x] `LoginScreen` shows a `Server access` choice with `Internet` and `Local network`, and new login sessions default to `Internet`.
- [x] ShelfDroid persists the selected server access mode with the current **Audiobookshelf server**, restores it on later sign-in attempts, and disables the selector during **Forced re-login**.

## Gate login discovery and password sign-in with local-network permission

**What to build:** When the user selects `Local network`, ShelfDroid requests Android 17 local-network permission before running **Login discovery** and **Password sign-in**. If permission is granted, ShelfDroid resumes the pending action automatically. If permission is denied, the user stays on `LoginScreen` with clear recovery guidance, including a settings call-to-action when the denial becomes permanent.

**Blocked by:** Add explicit server access mode to LoginScreen.

- [x] Selecting `Local network` causes ShelfDroid to request Android 17 local-network permission before **Login discovery** and **Password sign-in**.
- [x] After grant, the pending login action resumes automatically; after denial, `LoginScreen` stays in place with clear guidance and a settings call-to-action for permanent denial.

## Carry local-network access mode through OpenID login

**What to build:** The same explicit server access mode and local-network permission behavior works end to end for **OpenID login**, including the browser launch path and the callback completion path. If the user chose `Internet` and the login-time request fails, ShelfDroid may suggest trying `Local network`, but it must not switch modes automatically.

**Blocked by:** Gate login discovery and password sign-in with local-network permission.

- [x] **OpenID login** start and **OpenID callback completion** use the selected server access mode and request local-network permission when `Local network` is selected.
- [x] Failed login-time requests in `Internet` mode may suggest trying `Local network`, but ShelfDroid does not silently switch modes or retry with a different mode.
