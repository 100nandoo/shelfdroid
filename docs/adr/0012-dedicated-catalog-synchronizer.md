# Dedicated Catalog synchronizer

ShelfDroid separates Home screen retrieval from **Catalog synchronization**. `HomeRepository` exposes a local, reactive Home read model, while `CatalogSynchronizer` in `core-data.catalog` coordinates remote-to-local reconciliation through `LibraryRepository` and `LibraryItemRepository`; `HomeViewModel` depends on both responsibilities explicitly. Listening-stat refresh remains separate, and admin-only User and tag refresh belongs to `AdminDataRefresher` rather than the Catalog boundary. This keeps Catalog synchronization reusable outside Home without introducing the broader domain layer postponed by ADR 0009.

**Consequences**

Concurrent requests share one in-flight synchronization. Synchronization stops if Libraries cannot be refreshed, applies each Library independently after all of that Library's item batches succeed, and returns a structured result so Home can retain cached content while showing refresh progress or partial failure.
