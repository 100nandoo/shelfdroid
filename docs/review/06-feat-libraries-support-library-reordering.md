## Commit

**SHA:** `025162d15cc533b96561e917fdc1fd7a8eb1e72b`

**Title:** `feat(libraries): support library reordering`

## Commit Objective

Allow administrators to reorder Libraries with accessible buttons or drag gestures, show the change optimistically, and reconcile the list with the complete order accepted by Audiobookshelf. Reordering is gated on a connected, fully idle Library set and rolls back visibly when persistence fails.

The commit also serializes reorders with other Library operations, persists server display order in the catalog database, and adds coverage for the network contract, concurrency, UI interactions, optimistic state, failure recovery, and overlapping intents.

## Substantive Changes

### `LibraryRepository`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/library/LibraryRepository.kt
```
</details>

#### What changed

Extracts Library catalog writes from `fetchLibraries` into a reusable `persistLibraries` operation and includes each server Library's `displayOrder` in the stored entity.

#### Objective

Let successful reorder responses update the local catalog directly while preserving the existing transactional cleanup-and-insert behavior used by a full fetch.

### `LibraryAdministrationContract`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationContract.kt
```
</details>

#### What changed

Adds a reorder operation that accepts a complete administration list and returns the server-authoritative order, with an unsupported default for implementations that do not provide it. It also defines explicit unknown, connected, and disconnected connection states and unknown, idle, and active Library task states.

#### Objective

Expose reordering through a testable data contract and provide the state vocabulary needed to prevent unsafe moves while connectivity or server activity is uncertain.

### `LibraryAdministrationRepository`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationRepository.kt
```
</details>

#### What changed

Runs Library loads through the shared mutation coordinator and implements reordering inside the same gate. The repository maps the proposed list to one-based `newOrder` request entries, submits the complete order, persists the accepted response to the catalog, and maps that response back to administration models.

#### Objective

Prevent refresh, create, delete, and reorder work from racing while treating the server response as the source of truth for both UI state and local browsing order.

### `LibraryAdministrationUiState`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationUiState.kt
```
</details>

#### What changed

Extends screen state with connection status, per-Library task status, reorder progress, and reorder failure details. The new `canReorder` policy requires a connected session, a known target Library, and an idle task snapshot for every Library in the ordered set.

#### Objective

Keep reordering disabled unless moving a row cannot indirectly shift a Library whose server task state is active or unknown, and expose enough state for progress and recovery feedback.

### `LibraryMutationCoordinatorTest`

<details>
<summary>Paths</summary>

```text
core-data/src/test/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryMutationCoordinatorTest.kt
```
</details>

#### What changed

Adds a coroutine test that holds a create mutation open, verifies a reorder cannot enter the coordinator concurrently, then confirms the operations complete in create-before-reorder order.

#### Objective

Protect the global serialization guarantee relied on by reorder persistence and catalog reconciliation.

### `LibraryEntity` display order

<details>
<summary>Paths</summary>

```text
core-database/src/main/sqldelight/dev/halim/shelfdroid/core/database/LibraryEntity.sq
core-database/src/main/sqldelight/migrations/4.sqm
```
</details>

#### What changed

Adds a non-null `displayOrder` column to the Library table and migration, writes it during entity replacement, and orders both full-entity and ID queries by display order with an ID tie-breaker. Existing rows receive a default order of zero during migration.

#### Objective

Make the accepted Library order survive local persistence and produce deterministic catalog reads, including for pre-migration rows that share the default value.

### Library reorder API contract

<details>
<summary>Paths</summary>

```text
core-network/src/main/java/dev/halim/core/network/ApiService.kt
core-network/src/main/java/dev/halim/core/network/request/ReorderLibraryRequest.kt
```
</details>

#### What changed

Adds `POST api/libraries/order`, accepting an array of serializable entries whose wire fields are `id` and `newOrder`, and returning a `LibrariesResponse` containing the accepted order.

#### Objective

Represent Audiobookshelf's complete-order endpoint and payload explicitly in the network layer.

### `LibraryAdministrationApiServiceTest`

<details>
<summary>Paths</summary>

```text
core-network/src/test/java/dev/halim/core/network/LibraryAdministrationApiServiceTest.kt
```
</details>

#### What changed

Adds a transport test that captures a reorder request and verifies its HTTP method, endpoint path, complete JSON array, and successful decoding of an ordered Libraries response.

#### Objective

Lock down the exact Retrofit and serialization contract expected by the server.

### `LibraryAdministrationItem`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationItem.kt
core-ui/src/main/res/values/strings.xml
```
</details>

#### What changed

Adds optional move-up and move-down actions with localized visible labels and content descriptions, boundary-aware enabled states, and long-press vertical drag handling. Drag distance is accumulated until a row-step threshold is crossed, then emits one directional move and resets the accumulator. A localized rollback message is also added for reorder failures.

#### Objective

Offer both touch and accessible explicit controls while avoiding server-request churn from small pointer movements and providing clear failure feedback.

### `LibraryAdministrationScreen`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationScreen.kt
```
</details>

#### What changed

Renders the Library list with indices, derives each row's move boundaries and reorder eligibility, dispatches directional move events from buttons and drag gestures, and shows the reorder rollback message when an error is present.

#### Objective

Connect the new row interactions and state policy to the administration event flow without placing persistence logic in Compose.

### `LibraryAdministrationContentTest`

<details>
<summary>Paths</summary>

```text
core-ui/src/androidTest/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationContentTest.kt
```
</details>

#### What changed

Adds Compose tests that configure a connected, idle Library set, verify accessible reorder actions and row-boundary behavior, and simulate a long-press downward drag to assert the corresponding move event.

#### Objective

Verify that the rendered controls and gesture path expose the intended ViewModel events under reorder-eligible conditions.

### `LibraryAdministrationViewModel`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationViewModel.kt
```
</details>

#### What changed

Adds events for relative and destination-index moves, connection and task-state updates, and clearing reorder errors. Eligible moves reorder the list optimistically and launch persistence; accepted server responses become the rollback baseline, while the latest active failure restores that baseline and exposes an error. Load and intent generation counters prevent stale refreshes or older reorder acknowledgements from replacing newer user intent, and refreshes retain existing content while in progress.

#### Objective

Provide immediate reorder feedback while keeping overlapping refreshes and reorder requests deterministic, reconciling to server authority, and recovering safely from rejection.

### `LibraryAdministrationViewModelTest`

<details>
<summary>Paths</summary>

```text
core-ui/src/test/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationViewModelTest.kt
```
</details>

#### What changed

Adds tests and controllable repositories for optimistic moves followed by server-authoritative success, rollback with a visible error, gating under unknown, active, or disconnected state, and out-of-order completion where the newest accepted reorder must win.

#### Objective

Cover the primary success and failure paths plus the concurrency edge cases that could otherwise restore stale order.

### Library reorder delivery ticket

<details>
<summary>Paths</summary>

```text
docs/scratch/library-administration/06-reorder-libraries.md
```
</details>

#### What changed

Adds and completes the local implementation ticket, recording requirements for drag and accessible controls, optimistic rollback, serialized mutations, task-state gating, server-authoritative reconciliation, catalog persistence, and test coverage.

#### Objective

Capture the delivered scope and its dependencies in the repository's issue-tracking format.

### `FakeApiService`

<details>
<summary>Paths</summary>

```text
test-app/src/main/java/dev/halim/shelfdroid/test/app/testdi/FakeApiService.kt
```
</details>

#### What changed

Implements reorder behavior in the test API by applying requested orders by Library ID, retaining current order values for omitted entries, sorting by the resulting display order, and returning the reordered Libraries response.

#### Objective

Keep the test application compatible with the expanded API and provide deterministic server-like behavior for reorder flows.
