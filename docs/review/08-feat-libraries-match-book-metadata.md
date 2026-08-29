## Commit

**SHA:** `b6540a44c07ddbffa9f9960215532a7b11007ff4`

**Title:** `feat(libraries): match book metadata`

## Commit Objective

Add a Book-only library action that starts Audiobookshelf's bulk metadata-matching operation and
tracks it through the same durable server-task workflow used by library scans. The change keeps
scan and match operations mutually exclusive per library, distinguishes match progress and results
in the UI, and recovers completion, notification, and catalog synchronization when socket events
are missed.

## Substantive Changes

### `ApiService` and `ServerTaskApi`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/task/ServerTaskDependencies.kt
core-network/src/main/java/dev/halim/core/network/ApiService.kt
core-network/src/test/java/dev/halim/core/network/LibraryAdministrationApiServiceTest.kt
test-app/src/main/java/dev/halim/shelfdroid/test/app/testdi/FakeApiService.kt
```
</details>

#### What changed

Adds `matchLibrary` to the network and server-task API contracts. The Retrofit service sends a
`GET` request to `/api/libraries/{libraryId}/matchall`, the production task adapter delegates to
that request, and the test-app fake supplies a successful implementation. The network test verifies
both the HTTP method and encoded endpoint path.

#### Objective

Expose Audiobookshelf's match-all operation through the same injectable API boundary as task
snapshots and library scans, while locking its wire contract down with a request-level test and
keeping the test application compatible with the expanded service interface.

### `LibraryAdministrationContract` and `LibraryAdministrationRepository`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationContract.kt
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationRepository.kt
```
</details>

#### What changed

Extends the library-administration contract with `startMatch`, including the contract's existing
unsupported-operation default, and implements it by forwarding the selected library ID to
`ServerTaskRepository.startLibraryMatch`.

#### Objective

Make metadata matching available to presentation code without coupling the ViewModel to network or
server-task implementation details.

### `LibraryAdministrationUiState`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationUiState.kt
core-data/src/test/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationTaskStateTest.kt
```
</details>

#### What changed

Adds a dedicated generic match-start error and `matchError` state. The new `canStartMatch` policy
allows matching only while connected, only for Book libraries, and only when that library's shared
task state is idle. Unit coverage exercises the media-type, connection, and active-task gates.

#### Objective

Centralize match eligibility and error representation so every UI entry point applies the same
Book-only, per-library concurrency rules as the rest of library administration.

### `ServerTaskRepository`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/task/ServerTaskRepository.kt
core-data/src/test/java/dev/halim/shelfdroid/core/data/task/ServerTaskRepositoryBehaviorTest.kt
```
</details>

#### What changed

Generalizes scan-specific pending and accepted-placeholder tracking into operation-agnostic task
tracking. Scan and match now share `startLibraryOperation`, which rejects a second pending or active
operation for the same library, retains an accepted placeholder across the immediate recovery
snapshot, and records the correct `library-scan` or `library-match-all` action.

Match completion notifications now retain the operation action so delayed UI presentation can
select match-specific copy. Snapshot refreshes also detect terminal transitions, enqueue their
one-time notifications, and start or resume catalog synchronization for completed tasks, covering
the case where the socket completion event was missed. The behavior tests cover placeholder
replacement, same-library mutual exclusion and cross-library independence, synchronization failure
and retry without a second match request, delayed match notification identity, and refresh-based
completion recovery.

#### Objective

Reuse the durable scan task machinery for matching without allowing overlapping operations to race
on one library, and keep task completion, user notification, and local catalog state accurate across
HTTP/socket timing gaps, navigation, and reconnection.

### `LibraryAdministrationViewModel`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationViewModel.kt
core-ui/src/test/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationViewModelTest.kt
```
</details>

#### What changed

Introduces `StartMatch` event handling. The ViewModel checks `canStartMatch`, clears the previous
match-start error, invokes the repository, and converts unsafe failures to the localized generic
match error. Tests verify that only Book libraries issue requests, an active library task blocks
both scan and match, and match failures are represented without exposing internal exception text.

#### Objective

Connect the UI action to the data layer while preserving media-type and concurrency guards and the
existing safe error-reporting boundary.

### `LibraryAdministrationItem` and `LibraryAdministrationScreen`

<details>
<summary>Paths</summary>

```text
core-ui/src/androidTest/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationContentTest.kt
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationItem.kt
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationScreen.kt
core-ui/src/main/res/values/strings.xml
```
</details>

#### What changed

Adds an enabled-state-aware tonal match icon, tooltip, and content description to Book library rows
and emits `StartMatch` when selected; Podcast rows do not render the action. Task presentation now
uses match-specific active, completed, failed, cancelled, count, and elapsed strings for
`library-match-all`, while task-generic error and synchronization strings serve both operations.

The screen displays match-start failures and selects match-specific snackbar copy using the action
stored in the notification, with the current task as a compatibility fallback. Compose tests verify
Book-only action visibility and dispatch, match result/count/elapsed rendering, synchronization
retry dispatch, and localized generic failures.

#### Objective

Give administrators a direct and accessible metadata-matching control and clearly distinguish match
progress and outcomes from scan results, including delayed completion notifications and recoverable
local synchronization failures.

### Library administration ticket 08

<details>
<summary>Paths</summary>

```text
docs/scratch/library-administration/08-match-book-metadata.md
```
</details>

#### What changed

Adds the completed implementation ticket for matching missing Book metadata. It records the
Book-only action, shared per-library operation gate, durable task recovery, result and notification
requirements, separate synchronization retry behavior, and expected test coverage.

#### Objective

Document the delivered feature scope and its acceptance criteria as the next completed step in the
library-administration ticket series.
