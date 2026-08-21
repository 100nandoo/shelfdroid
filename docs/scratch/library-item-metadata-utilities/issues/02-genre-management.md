# 02 — Global Genre management

**What to build:** Admin and root Users can manage server-wide **Genres** from the **Library item metadata utilities** hub, with the same deliberate bulk-change experience as Tag management.

**Blocked by:** 01 — Admin hub and global Tag management.

**Status:** ready-for-agent

- [ ] The Genre destination is reachable from the administrator-only hub and handles loading, empty, failure, access-denied, and ready states accessibly.
- [ ] Genres load in case-insensitive alphabetical order from the Audiobookshelf server.
- [ ] Genre rename rejects blank values, warns about exact and case-only collisions, confirms its all-matching-Library-item effect, and reports the returned updated-item count.
- [ ] Genre delete uses the server-compatible encoded path form for spaces, punctuation, and non-ASCII values, requires confirmation, and reports the returned updated-item count.
- [ ] A completed mutation refreshes the visible Genre list; failures leave the current list intact and provide an operation-specific recovery message.
- [ ] Production and fake authenticated server contracts, repository behavior tests, and focused screen tests cover success, access denial, encoding, confirmation, collision, and failure behavior.
