## Commit

**SHA:** `08579887f7a6bf95932a56354ba8497ec8cb7d3e`

**Title:** `feat(libraries): delete libraries safely`

## Commit Objective

Add an explicit, guarded Library-deletion workflow to the administration screen. The workflow only permits deletion for an idle Library while the server-task connection is available, requires confirmation that catalog data will be removed while media files remain, serializes the server mutation, and exposes a safe retry path on failure.

After the server accepts deletion, the commit removes the Library and its items from ShelfDroid's local catalog projection without issuing playback-stop or downloaded-media cleanup commands. Home tracks the selected Library by ID so catalog updates retain the current selection when possible, select an adjacent Library when the active one disappears, and show the empty state after the last Library is removed.

## Substantive Changes

### `ApiService`

<details>
<summary>Paths</summary>

```text
core-network/src/main/java/dev/halim/core/network/ApiService.kt
core-network/src/test/java/dev/halim/core/network/LibraryAdministrationApiServiceTest.kt
```
</details>

#### What changed

Adds `deleteLibrary`, a Retrofit `DELETE api/libraries/{libraryId}` request that returns the deleted server `Library`. The network test captures the outgoing request and verifies both the HTTP method and encoded path.

#### Objective

Provide the network contract used by administration to remove a Library's catalog records, with a focused test preventing endpoint or verb regressions.

### `FakeApiService`

<details>
<summary>Paths</summary>

```text
test-app/src/main/java/dev/halim/shelfdroid/test/app/testdi/FakeApiService.kt
```
</details>

#### What changed

Makes the fake server's Library collection mutable, restores its initial contents during `reset`, and implements `deleteLibrary`. The fake removes and returns a known Library or returns an `Unknown library` failure for an unrecognized ID.

#### Objective

Let the test application exercise persistent delete behavior while keeping repeated test-app sessions deterministic and providing a realistic missing-Library failure path.

### `LibraryItemRepository`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/library/LibraryItemRepository.kt
```
</details>

#### What changed

Adds `removeLibraryFromCatalog`, which finds all item IDs associated with a Library, removes those items and their related local records in one database transaction through the existing in-transaction deletion path, then removes their progress records. It returns immediately when the Library has no local items.

#### Objective

Reconcile the local item projection after server deletion without invoking downloaded-media or playback cleanup, preserving media files and buffered playback as required by the deletion contract.

### `LibraryRepository`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/library/LibraryRepository.kt
```
</details>

#### What changed

Adds `removeFromCatalog`, which deletes a Library row from the local database by ID.

#### Objective

Ensure the locally observed Library list reflects a successful server deletion immediately.

### `LibraryAdministrationContract`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationContract.kt
```
</details>

#### What changed

Extends the administration contract with `deleteLibrary(libraryId)`. Its default implementation returns an unsupported-operation failure, matching the contract's compatibility pattern for optional administration mutations.

#### Objective

Expose Library deletion to the presentation layer without forcing every partial or test implementation to support the operation immediately.

### `LibraryAdministrationRepository`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationRepository.kt
```
</details>

#### What changed

Injects `LibraryItemRepository` and implements `deleteLibrary` inside `LibraryMutationCoordinator.withMutation`. On a successful server response, it removes the Library's item projection and then its Library row from the local catalog; a server failure leaves the local projection unchanged.

#### Objective

Serialize deletion with other Library mutations and make server success the boundary for local reconciliation, while deliberately retaining server media, downloaded media, and active playback.

### `LibraryAdministrationUiState`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationUiState.kt
```
</details>

#### What changed

Adds a generic deletion error and state for the pending confirmation, in-flight Library ID, and retry target. The new `canDelete` predicate requires a connected task stream, a known Library with an idle task state, no active deletion, and no reorder in progress. Reordering is also disabled while deletion is active or its confirmation is open.

#### Objective

Centralize the safety gates and transient state needed to prevent deletion from racing active server tasks or Library reordering.

### `LibraryAdministrationViewModel`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationViewModel.kt
core-ui/src/test/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationViewModelTest.kt
```
</details>

#### What changed

Adds request, cancel, confirm, and retry deletion events and their state transitions. Confirmation revalidates the deletion gate before launching the repository call. Success removes the Library from the displayed and last-known server lists, clears its task state and task records, and resets deletion state; failure preserves the Library, exposes a generic error, and records the Library ID for a confirmation-based retry.

Unit tests cover confirmation and cancellation, unknown/active/disconnected task gating, successful removal, final-Library removal, and failure with a safe retry prompt. The fake repository records deletion requests and supplies controlled results.

#### Objective

Coordinate deletion as an explicit, recoverable user workflow and ensure stale or unsafe confirmation state cannot bypass the latest task and connectivity checks.

### `LibraryAdministrationItem`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationItem.kt
```
</details>

#### What changed

Adds `deleteEnabled` and `onDelete` inputs and renders a tonal delete icon button with a tooltip and content description alongside the existing row actions.

#### Objective

Make deletion directly discoverable and accessible on each Library row while allowing screen state to disable unsafe actions.

### `LibraryAdministrationContent`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationScreen.kt
core-ui/src/main/res/values/strings.xml
core-ui/src/androidTest/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationContentTest.kt
```
</details>

#### What changed

Wires each row's delete action to the ViewModel events using `canDelete`, displays a confirmation dialog for the selected Library, and adds an inline generic deletion error with a retry button. New strings identify the action, name the target Library in the dialog, explain that catalog data is removed while media files remain, and provide an actionable failure message. Compose tests verify that idle and active Libraries receive the correct enabled state, the button emits the request event, and confirmation text and acceptance dispatch the confirm event.

#### Objective

Present the destructive action with clear consequences, accessible controls, state-derived gating, and a safe recovery path without exposing internal failure details.

### `HomeUiState`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/home/HomeUiState.kt
core-data/src/test/java/dev/halim/shelfdroid/core/data/screen/home/HomeLibrarySelectionTest.kt
```
</details>

#### What changed

Tracks `activeLibraryId` separately from the pager position and adds `reconcileActiveLibraryId`. The reducer preserves an existing active ID, selects the Library that moves into a deleted Library's former position, falls back to the preceding final item, chooses the first Library when no prior selection can be resolved, and returns `null` for an empty catalog.

Tests cover deletion of an inactive Library, middle and final active Libraries, deletion of the only Library, and the absence of playback-stop or cleanup commands from the pure selection transition.

#### Objective

Keep Home selection stable across catalog updates and Misc/administration navigation while providing deterministic adjacent-Library and empty-state fallbacks without coupling selection changes to playback teardown.

### `HomeViewModel`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/HomeViewModel.kt
```
</details>

#### What changed

Replaces the externally combined and subscription-scoped UI state with a single exposed `MutableStateFlow` updated by a long-lived repository collection. Each Library-list emission reconciles `activeLibraryId` before replacing the list, and pager change events update both the numeric page and the selected ID when the page represents a Library; Misc pages retain the last Library ID.

#### Objective

Make Library identity, rather than a shifting page index, the source of truth when deletion changes the observed catalog, and continue observing those changes independently of temporary UI subscriptions.

### `HomeScreen`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/HomeScreen.kt
```
</details>

#### What changed

Shows the no-Libraries message before constructing a pager with invalid Library content, and returns from `HomeScreenContent` after rendering that empty state. A new effect maps the reconciled active Library ID back to its current page and scrolls there when needed; the scroll does not dispatch player events. The success-empty check now uses the actual Library list rather than the supplied pager count, which includes the Misc page.

#### Objective

Render a valid empty state after final-Library deletion and move Home to the correct adjacent Library after other deletions while preserving buffered playback.

### Delete Library delivery ticket

<details>
<summary>Paths</summary>

```text
docs/scratch/library-administration/09-delete-library.md
```
</details>

#### What changed

Adds the completed local issue describing the delete workflow, its dependencies, safety requirements, local-selection and playback expectations, error behavior, and intended test coverage.

#### Objective

Record the delivered behavior and acceptance criteria for safely deleting a Library as part of the administration feature series.
