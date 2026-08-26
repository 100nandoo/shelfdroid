# 05 — Preserve accepted Library deletion after synchronization failure

**What to build:** Once the Audiobookshelf server deletes a Library, ShelfDroid treats the deletion
as accepted even if local synchronization fails and lets the administrator retry synchronization
without sending another delete request.

**Blocked by:** 04 — Preserve accepted Library reordering after synchronization failure.

**Status:** ready-for-agent

- [ ] Delete results reuse the partial-success outcome established for accepted Library mutations.
- [ ] A server-accepted deletion immediately removes the Library from administration and applies
      the correct active-Library fallback.
- [ ] Local synchronization failure is reported distinctly and retry performs no additional delete
      request.
- [ ] Tests cover server rejection, accepted deletion, partial success, final-Library deletion, and
      synchronization retry.
