# 01 — Admin hub and global Tag management

**What to build:** Admin and root Users can reach the **Library item metadata utilities** hub from server administration and fully manage server-wide **Tags**. The Tag screen presents loaded, empty, loading, failure, and access-denied states; supports rename and delete; makes every bulk change deliberate; and keeps ShelfDroid's administrative Tag cache current.

**Blocked by:** None — can start immediately.

**Status:** ready-for-human

- [x] Only admin and root Users see the new hub and Tag destination; locally known non-admin Users make no metadata-utility request, and server access denial is shown clearly.
- [x] The Tag screen loads and case-insensitively sorts server Tags, with accessible loading, empty, retryable failure, and ready states.
- [x] Tag rename rejects blank input, warns about exact and case-only collisions, confirms the all-matching-Library-item effect, and reports the server's updated-item count.
- [x] Tag delete encodes the selected value safely for the Audiobookshelf request, requires an all-matching-Library-item confirmation, and reports the completed count.
- [x] Successful Tag mutations refresh both the visible list and ShelfDroid's administrative Tag cache; failed mutations preserve canonical cached values.
- [x] Production and fake authenticated server contracts, repository behavior tests, and focused UI or navigation tests cover the delivered behavior.
