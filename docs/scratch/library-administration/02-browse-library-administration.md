# 02 — Browse Library administration

**What to build:** Admin/root users can open a dedicated Library administration screen, view the
server's ordered Libraries, and explicitly refresh them.

**Blocked by:** None — can start immediately.

**Status:** complete

- [x] The existing Libraries administration entry opens the screen for admin/root users only.
- [x] A successful request displays Libraries in server order with their type and identity.
- [x] Pull-to-refresh reloads Libraries and clearly represents loading and refresh failure.
- [x] Initial request failure shows an unavailable state with retry and does not use cached names.
- [x] Tapping a Library row has no effect and does not create selected-row state.
- [x] Empty state and no-current-Library state remain usable.
- [x] Functional and accessibility tests cover access control, loading, empty, unavailable, retry,
      ordered content, and inert row taps.
