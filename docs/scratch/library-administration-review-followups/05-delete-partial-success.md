# 05 — Preserve accepted Library deletion after synchronization failure

**What to build:** Once the Audiobookshelf server deletes a Library, ShelfDroid treats the deletion
as accepted even if local synchronization fails and lets the administrator retry synchronization
without sending another delete request.

**Blocked by:** 04 — Preserve accepted Library reordering after synchronization failure.

**Status:** complete

- [x] Delete results reuse the partial-success outcome established for accepted Library mutations.
- [x] A server-accepted deletion immediately removes the Library from administration and applies
      the correct active-Library fallback.
- [x] Local synchronization failure is reported distinctly and retry performs no additional delete
      request.
- [x] Tests cover server rejection, accepted deletion, partial success, final-Library deletion, and
      synchronization retry.

**Verification:** `:core-data:testDebugUnitTest`, `:core-ui:testDebugUnitTest`,
`:test-app:compileDebugKotlin`, and `:core-ui:compileDebugAndroidTestKotlin` pass. Focused unit,
repository, and Compose tests cover server rejection, accepted and final-Library deletion, local
and synchronization post-acceptance failures, cancellation propagation, synchronization retry, and
no repeated delete request. Connected instrumentation was not run because no Android device or
emulator was available.
