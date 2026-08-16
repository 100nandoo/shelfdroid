# Dedicated Library data repository

ShelfDroid separates Home screen retrieval from **Library data synchronization**. `HomeRepository` exposes a local, reactive Home read model, while `LibraryDataRepository` in `core.data.library` coordinates remote-to-local reconciliation through `LibraryRepository` and `LibraryItemRepository`; `HomeViewModel` depends on both responsibilities explicitly. Listening-stat refresh remains separate, and admin-only User and tag refresh belongs to `AdminDataSynchronizer` rather than the Library data boundary. This keeps Library data synchronization reusable outside Home without introducing the broader domain layer postponed by ADR 0009.

**Consequences**

Concurrent requests share one in-flight synchronization. Synchronization stops if Libraries cannot be refreshed, applies each Library independently after all of that Library's item batches succeed, and returns a structured result so Home can retain cached content while showing refresh progress or partial failure.
