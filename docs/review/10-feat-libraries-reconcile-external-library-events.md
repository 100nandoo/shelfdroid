## Commit

**SHA:** `1e8f9fb9f7e8eb553b62bf4afed4ed945cb4bc74`

**Title:** `feat(libraries): reconcile external library events`

## Commit Objective

Keep the local catalog and Library administration screen synchronized with Library additions, updates, and removals made by other clients. The commit introduces an application-scoped Socket.IO event owner that parses Library events, deduplicates repeated deliveries and local mutation echoes, serializes them with in-app mutations, and converges on an authoritative server snapshot without polling.

The reconciled snapshots are exposed through the existing Library administration boundary and applied defensively in the UI so stale HTTP responses or optimistic reorder intents cannot overwrite a newer external change. Removal events clean stale catalog projections immediately, reconnects force recovery synchronization, and reconciliation failures preserve the prior list while exposing the existing retryable unavailable state.

## Substantive Changes

### `LibraryAdministrationLibraryEvent`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationLibraryEvent.kt
```
</details>

#### What changed

Adds event types for Library additions, updates, removals, and reconnect refreshes, plus a reconciled event model containing the catalog-safe Library projection, authoritative post-sync list, synchronization outcome, and an internal payload fingerprint. The parser accepts the three supported socket event names, rejects missing, blank, unknown, or malformed payloads, decodes the server `Library`, maps only catalog-facing fields, and derives normalized deduplication keys from the decoded model rather than raw JSON formatting.

#### Objective

Define a narrow event boundary that can recognize semantically duplicate deliveries while avoiding retention or exposure of rich server-side Library configuration in UI-facing state.

### `LibraryAdministrationEventReconciler`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationEventReconciler.kt
```
</details>

#### What changed

Introduces a reconciler that shares `LibraryMutationCoordinator` with create, delete, and reorder operations, bounds its recent-event history, and temporarily records successful local mutations so their socket echoes are consumed instead of causing a second synchronization. Reconnect refreshes always pass through as recovery boundaries. Accepted removal events delete the Library's items and Library projection before requesting a full synchronization, while every successful reconciliation returns the current authoritative Library list and failures propagate to the publisher.

#### Objective

Make external events, duplicate socket deliveries, local HTTP/socket races, and reconnect recovery converge deterministically without polling or allowing concurrent mutation paths to overwrite one another. Immediate removal cleanup prevents a confirmed server deletion from leaving stale catalog content when the follow-up snapshot fails.

### `LibraryAdministrationEventRepository` and `LibraryAdministrationEventOwner`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationEventRepository.kt
```
</details>

#### What changed

Adds a singleton repository that owns a buffered shared event flow, constructs the reconciler from the catalog repositories and synchronizer, and converts successful in-app additions and removals into matching local-echo registrations. Its socket owner acquires an independent shared connection handle, subscribes to Library add/update/remove events plus `connect`, launches accepted reconciliations on the injected I/O scope, and releases only its own subscriptions and ownership handle when closed. Successful work publishes the complete synchronized list; failures publish a sanitized unsuccessful event with no partial list or internal transport error.

#### Objective

Give Library events an application-scoped owner that survives administration-screen recreation, coexists with task and podcast socket consumers, recovers missed events after reconnect, and exposes only safe, fully reconciled state to presentation code.

### Library administration event bridge

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationContract.kt
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationRepository.kt
```
</details>

#### What changed

Extends `LibraryAdministrationContract` with a `SharedFlow` of reconciled Library events and a default empty flow for compatible fakes and alternate implementations. The production repository exposes the singleton event repository's flow and registers successful delete and create responses before applying their local catalog changes, allowing the later server echo to be recognized as the same mutation.

#### Objective

Carry external-change reconciliation through the existing Library administration abstraction while preventing successful in-app mutations from triggering redundant socket-driven refreshes.

### `LibraryAdministrationViewModel`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationViewModel.kt
```
</details>

#### What changed

Collects the new event flow for the ViewModel lifetime. A successful reconciliation invalidates older load and optimistic-intent generations, replaces the remembered server snapshot and visible list, clears refresh and reorder errors, and reapplies current task state against the new Libraries. A failed reconciliation keeps the existing Library list, exits refresh state, and switches to the existing generic unavailable state for user-driven retry.

#### Objective

Apply server-authoritative external changes without letting late HTTP responses or stale reorder intents undo them, while retaining known data and a safe recovery path when synchronization fails.

### `HomeViewModel`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/HomeViewModel.kt
```
</details>

#### What changed

Injects the singleton `LibraryAdministrationEventRepository` into the Home ViewModel even though the ViewModel does not call it directly. Resolving the dependency creates the application-scoped event owner when the main Home flow is constructed, rather than waiting until Library administration is opened.

#### Objective

Start listening for external Library changes during normal app use so catalog reconciliation is not limited to sessions in which the administration screen has already instantiated its repository.

### `LibraryAdministrationEventOwnerTest`

<details>
<summary>Paths</summary>

```text
core-data/src/test/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationEventOwnerTest.kt
```
</details>

#### What changed

Adds an integration-style socket-owner test with a fake shared socket. It verifies independent task, podcast, and Library ownership; one refresh on connection; no elapsed-time polling; duplicate add-event suppression; add/update/remove publication order; and cleanup that releases only the Library owner's handle while the other consumers continue receiving events.

#### Objective

Protect the socket lifecycle and subscription-coexistence boundary, including the failure-prone cases where a new consumer could replace listeners, disconnect shared users, or introduce periodic refreshes.

### `LibraryAdministrationLibraryEventTest`

<details>
<summary>Paths</summary>

```text
core-data/src/test/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationLibraryEventTest.kt
```
</details>

#### What changed

Adds tests for catalog projection mapping, updated and removed event types, invalid and unknown payload rejection, and fingerprints that retain changes in rich administration settings. Reconciler tests cover duplicate suppression, server-snapshot use, both orderings of the local-mutation echo race, non-deduplicated reconnect refreshes, and removal cleanup ordering before synchronization.

#### Objective

Lock down event parsing and the reconciliation race boundaries most likely to cause missed external changes, redundant work, or stale catalog data.

### `SocketManager.Event.Library`

<details>
<summary>Paths</summary>

```text
socketio/src/main/java/dev/halim/socketio/SocketManager.kt
```
</details>

#### What changed

Adds centralized constants for the server's `library_added`, `library_updated`, and `library_removed` Socket.IO event names.

#### Objective

Provide one typed event vocabulary for the new Library subscriber and its tests instead of duplicating protocol strings across layers.

### Library event reconciliation ticket

<details>
<summary>Paths</summary>

```text
docs/scratch/library-administration/10-reconcile-library-events.md
```
</details>

#### What changed

Adds and marks complete the implementation ticket for external Library reconciliation. It records the required add/update/remove behavior, catalog and active-Library convergence, shared-subscription safety, duplicate and missed-event recovery, and expected test coverage.

#### Objective

Document the delivered behavior and its acceptance criteria within the repository's local Library administration work series.
