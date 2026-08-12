---
title: Spec: Login-time server access mode for local-network servers
labels:
  - ready-for-human
related:
  - "#289"
---

# Spec: Login-time server access mode for local-network servers

## Problem Statement

Users who host an **Audiobookshelf server** on their local network can fail to sign in on Android 17 devices even though the same server works when reached over the Internet. ShelfDroid currently treats login as a normal Internet request path, but Android 17 requires explicit local-network permission when the app reaches a server on the local network. Users need a clear, predictable way to tell ShelfDroid how to reach their server during login without relying on hidden network heuristics.

## Solution

ShelfDroid will add an explicit `Server access` choice to `LoginScreen`, with `Internet` and `Local network` options. When the user selects `Local network`, ShelfDroid will request Android 17 local-network permission before running login-time server calls such as **Login discovery**, **Password sign-in**, and **OpenID login**. ShelfDroid will remember the selected access mode for the current **Audiobookshelf server**, automatically reuse it during **Forced re-login**, and prevent changes to that setting during re-login.

## User Stories

1. As a first-time user with a public **Audiobookshelf server**, I want ShelfDroid to default to `Internet`, so that normal sign-in stays simple.
2. As a first-time user with a home-network **Audiobookshelf server**, I want to explicitly choose `Local network`, so that ShelfDroid can ask for the right permission before login fails.
3. As a user entering a server address, I want the `Server access` choice to be visible on `LoginScreen`, so that I can understand how ShelfDroid plans to reach my server.
4. As a user signing in with username and password, I want local-network permission to be handled before the request is sent, so that my sign-in attempt does not fail for avoidable platform reasons.
5. As a user signing in with **OpenID login**, I want the same local-network handling to apply, so that the alternative login method works consistently with the selected server access mode.
6. As a user whose server only supports **OpenID login**, I want ShelfDroid to request local-network permission before **Login discovery** if I selected `Local network`, so that the login method surface can still load correctly.
7. As a returning user in **Forced re-login**, I want ShelfDroid to preserve the previously chosen server access mode, so that I do not have to re-decide how my current **Audiobookshelf server** is reached.
8. As a returning user in **Forced re-login**, I want the `Server access` choice to be read-only, so that the relogin flow stays focused on re-authenticating the same **User** on the same **Audiobookshelf server**.
9. As a user returning from the browser during **OpenID login**, I want ShelfDroid to continue using the same server access mode, so that the callback completion path behaves consistently.
10. As a user who grants local-network permission, I want ShelfDroid to automatically resume the pending login action, so that I do not need to tap the same control twice.
11. As a user who denies local-network permission once, I want to stay on `LoginScreen` and see a clear explanation, so that I understand why the login flow cannot continue.
12. As a user who permanently denies local-network permission, I want a visible settings shortcut on `LoginScreen`, so that I can recover without guessing where to change the permission.
13. As a user whose server is actually public, I want `Internet` mode to avoid unnecessary permission prompts, so that ShelfDroid remains low-friction for normal deployments.
14. As a user who mistakenly chose `Internet` for a server that is only reachable on the local network, I want ShelfDroid to suggest trying `Local network` after a failed login-time request, so that I can recover without debugging Android networking behavior myself.
15. As a user who edits the server field, I want stale **Login discovery** state to be cleared when the server changes, so that the server access choice works with the current server rather than leftover state.
16. As a user who signs in repeatedly to the same **Audiobookshelf server**, I want ShelfDroid to remember my chosen server access mode, so that I do not need to reconfigure it every time.
17. As a user who uses a public server and never selects `Local network`, I want the login flow to behave exactly as it does today, so that this fix does not regress normal Internet sign-in.
18. As a maintainer, I want the login-time local-network behavior to be explicit rather than inferred, so that the first fix is predictable and easy to support.

## Implementation Decisions

- The feature is intentionally scoped to login-time interactions only. It will not gate signed-in Home, administration, player, or background sync flows in this spec.
- `LoginScreen` will expose an explicit `Server access` choice with two values only: `Internet` and `Local network`.
- `Internet` will be the default access mode for new login sessions.
- The selected server access mode will be persisted with the current **Audiobookshelf server** and reused for future login attempts.
- During **Forced re-login**, ShelfDroid will reuse the saved **Audiobookshelf server** and saved server access mode, and the access-mode selector will be disabled.
- The login flow will treat the user-selected server access mode as the source of truth. This spec does not use DNS resolution, IP classification, hostname heuristics, or automatic access-mode switching.
- Login-time local-network permission handling will run before these actions:
  - **Login discovery**
  - **Password sign-in**
  - **OpenID login start**
  - **OpenID callback completion**
- The highest behavioral seam for this feature will remain the existing login state/event loop. New logic should be coordinated at the login flow boundary rather than buried in the network client.
- The login flow may introduce a narrow internal concept for a pending login action so that a permission request can pause and then resume the original action after grant.
- Android support in this spec is Android 17 local-network permission only. The feature will declare and request the Android 17 permission needed for local-network server access.
- If the user selects `Local network` and permission is missing, ShelfDroid will request permission before issuing the login-time server request.
- If permission is granted, ShelfDroid will automatically resume the pending action without requiring another tap.
- If permission is denied once, ShelfDroid will stay on `LoginScreen` and show a clear explanation of why local-network access is needed for the selected **Audiobookshelf server**.
- If permission is permanently denied, ShelfDroid will show a visible settings call-to-action on `LoginScreen`.
- If the user selected `Internet` and a login-time request fails, ShelfDroid may show a targeted hint suggesting `Local network`, but it must not switch modes automatically or silently retry with a different mode.
- The product wording will use `Local network` and `Server access`, not `Nearby devices`, because the user task is reaching an **Audiobookshelf server**, not managing generic nearby hardware.
- Existing login behavior for Internet-hosted servers should remain unchanged apart from the presence of the selector.

## Testing Decisions

- Good tests should assert external behavior of the login flow rather than implementation details. They should verify which login-time actions run, when permission is requested, what state the user sees, and whether a pending action resumes correctly.
- The primary test seam should be the existing login state/event boundary. Tests should prefer existing login view-model and login state seams over new lower-level seams.
- The login state and view-model tests should cover:
  - default `Internet` access mode for a new login session
  - persisted access mode restoration for a saved **Audiobookshelf server**
  - disabled access-mode selector during **Forced re-login**
  - permission-request event emission when `Local network` is selected
  - automatic resume after permission grant
  - denial and permanent-denial behavior
  - preservation of the selected mode across **OpenID login** start and callback completion
- The login screen state tests should cover rendering and copy decisions, including the presence of the selector, the disabled re-login state, and the settings call-to-action for permanent denial.
- Repository and coordinator tests should cover the existing login-time request surfaces that remain behaviorally important, especially **Login discovery** and **OpenID login** continuation.
- Prior art for these tests already exists in the codebase’s login test suite, including tests around login state initialization, login screen state behavior, repository-driven login flows, and **OpenID login** callback handling.
- Tests should not attempt to assert Android’s system permission dialog UI directly. Instead, they should assert the app-level permission request event and the resulting resumed or blocked flow.

## Out of Scope

- Automatically detecting whether the current **Audiobookshelf server** is on the local network.
- DNS, IP, or routing heuristics for classifying server access mode.
- Automatically flipping from `Internet` to `Local network` after a failed request.
- Applying the same gate to signed-in Home, administration, player, or background sync flows.
- Android 16 compatibility behavior for opt-in local-network restrictions.
- Any redesign of the broader authentication architecture beyond the login-time access-mode choice.

## Further Notes

- This spec intentionally chooses explicit user input over network inference for the first fix. That trade-off is recorded separately in the related architecture decision.
- The accepted testing seam assumption is to keep the feature at the existing login flow boundary instead of embedding permission logic inside the network client.
- A follow-up work item is expected for already-signed-in users who later move onto a local network and need the same protection outside `LoginScreen`.
