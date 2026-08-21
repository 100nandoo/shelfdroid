# 03 — Custom metadata provider management

**What to build:** Admin and root Users can list, add, reveal or hide authorization headers for, and safely delete Book **Custom metadata providers** from the **Library item metadata utilities** hub.

**Blocked by:** 01 — Admin hub and global Tag management.

**Status:** ready-for-agent

- [ ] The provider destination loads the server's provider list and presents accessible loading, empty, failure, access-denied, and ready states.
- [ ] Administrators can add a Book Custom metadata provider with required name and URL plus an optional authorization header; the result is reflected only after server success.
- [ ] Authorization-header entry and read-only display use password fields concealed by default, with accessible per-field eye toggles to reveal or hide the value.
- [ ] Authorization-header values are neither persisted nor logged, and leaving the provider screen clears transient values and resets all reveal state.
- [ ] The screen intentionally offers no provider edit action or delete-and-recreate substitute.
- [ ] Provider deletion requires a destructive confirmation that explains dependent Libraries will fall back to Audiobookshelf's Google metadata source, then reports the accurate server result without inventing a Library count.
- [ ] Production and fake authenticated server contracts, repository behavior tests, and focused UI tests cover provider lifecycle, secret handling, authorization, confirmation, and failure behavior.
