## Commit

**SHA:** `4ebf1d7a5ed19b40742edf9667bcd8714d18b79c`

**Title:** `feat(libraries): track library scan tasks`

## Commit Objective

Add a Library administration scan action whose progress follows Audiobookshelf's durable Server-task state rather than treating the scan request's HTTP response as completion. The change gives the application an operation-agnostic, application-scoped task owner that reconciles HTTP snapshots with socket events, survives screen recreation and reconnection, synchronizes the local catalog after successful scans, and exposes terminal results and notifications to the UI.

The UI gates scanning and reordering until connection and task state are trustworthy, distinguishes completed, failed, cancelled, and catalog-synchronization outcomes, and keeps unsafe internal error details out of user-visible messages.

## Substantive Changes

### Server-task network contracts

<details>
<summary>Paths</summary>

```text
core-network/src/main/java/dev/halim/core/network/ApiService.kt
core-network/src/main/java/dev/halim/core/network/response/Tasks.kt
```
</details>

#### What changed

Adds a `POST /api/libraries/{libraryId}/scan` request that models only request acceptance and a `GET /api/tasks` request for the current task snapshot. Introduces serializable, operation-agnostic task response models carrying task identity, action, arbitrary data, localized and plain-text metadata, failure/completion flags, and timestamps.

#### Objective

Provide the HTTP contracts needed to start a scan and recover authoritative task state without coupling network models to the Library administration UI or incorrectly inferring completion from scan acceptance.

### `LibraryAdministrationApiServiceTest`

<details>
<summary>Paths</summary>

```text
core-network/src/test/java/dev/halim/core/network/LibraryAdministrationApiServiceTest.kt
```
</details>

#### What changed

Adds endpoint tests that assert the scan request uses the expected POST path and the task snapshot uses the expected GET path. The test server also returns an empty task response for task requests.

#### Objective

Lock down the new Retrofit request contracts and verify that an accepted scan remains distinct from later task-state retrieval.

### `FakeApiService`

<details>
<summary>Paths</summary>

```text
test-app/src/main/java/dev/halim/shelfdroid/test/app/testdi/FakeApiService.kt
```
</details>

#### What changed

Implements the new API methods in the test application with successful scan acceptance and an empty task snapshot.

#### Objective

Keep the test application's `ApiService` implementation complete and allow Library administration flows to run without a live Server task backend.

### Server-task dependency seams

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/task/ServerTaskDependencies.kt
```
</details>

#### What changed

Introduces narrow interfaces and production adapters for task HTTP access, post-scan catalog synchronization, and time. The catalog adapter converts the existing synchronization result into `Result<Unit>`, while the clock adapter supplies wall-clock time for retention and race reconciliation.

#### Objective

Separate task reduction from concrete network, catalog, and time implementations so the repository can coordinate production behavior while remaining deterministic in tests.

### `ServerTaskSocket` and `SocketManager`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/task/ServerTaskSocket.kt
socketio/src/main/java/dev/halim/socketio/SocketManager.kt
```
</details>

#### What changed

Adds a task-specific socket abstraction and a `SocketManager` adapter for acquiring shared connection ownership, subscribing independently to events, and reading the current connection state. `SocketManager` gains a non-owning `isConnected()` probe used during repository initialization.

#### Objective

Let the task repository observe task and lifecycle events through the shared socket without taking exclusive listener ownership or waiting for a future connect callback when the socket is already connected.

### Server-task dependency wiring

<details>
<summary>Paths</summary>

```text
core-data/build.gradle.kts
core-data/src/main/java/dev/halim/shelfdroid/core/data/di/LibraryAdministrationModule.kt
```
</details>

#### What changed

Adds the socket module as a `core-data` dependency and binds the socket, HTTP API, catalog synchronizer, clock, and application-scoped repository implementations to their task abstractions through Hilt.

#### Objective

Make the new task-tracking graph available to Library administration using production dependencies while preserving injectable seams for tests.

### `ServerTaskRepository`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/task/ServerTaskRepository.kt
```
</details>

#### What changed

Introduces the application-scoped task domain model, connection/status/synchronization states, safe error representation, result counts, terminal notifications, and repository contract. The repository bootstraps from the task endpoint, refreshes on explicit requests and socket reconnection, reconciles authoritative snapshots with socket `task_started` and `task_finished` events, and does not poll.

Scan acceptance creates an active placeholder when the real task has not appeared yet, then reconciles that placeholder against task events and the immediate recovery snapshot without allowing request/event ordering races to fabricate or strand an active scan. Terminal tasks are retained for one minute, deduplicated into acknowledgeable notifications, and expired independently from notification delivery.

Completed tasks trigger catalog synchronization; synchronization failure is recorded separately from scan failure and can be retried without rerunning the scan. Payload mapping extracts the library, counts, elapsed time, timestamps, cancellation state, and display-safe server errors while replacing exception-like details with a generic error.

#### Objective

Create a durable single source of truth for asynchronous Server work so navigation, reconnects, refreshes, and HTTP/socket races cannot make Library scans appear complete, idle, or failed prematurely.

### Library administration task bridge

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationContract.kt
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationRepository.kt
```
</details>

#### What changed

Extends the Library administration contract with task state and notification flows plus refresh, scan, synchronization-retry, and acknowledgement operations. Default implementations preserve compatibility for existing fakes, while the production repository delegates every task operation to `ServerTaskRepositoryContract`.

#### Objective

Expose durable task tracking to the presentation layer through the existing Library administration boundary without making the ViewModel depend directly on task infrastructure.

### `LibraryAdministrationUiState`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationUiState.kt
```
</details>

#### What changed

Adds scan-start and synchronization error keys, the task list, terminal notification, and task-specific errors to UI state. New helpers permit scans only for connected, known-idle Book or Podcast libraries, select the active or latest task for a library, and map terminal task statuses back to an idle administration state.

#### Objective

Centralize scan eligibility and task presentation rules so unsafe or unsupported actions remain disabled and the UI consistently resolves the relevant task for each library.

### `LibraryAdministrationTaskStateTest`

<details>
<summary>Paths</summary>

```text
core-data/src/test/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationTaskStateTest.kt
```
</details>

#### What changed

Adds pure state tests covering the complete scan gate: disconnected, unknown-snapshot, active-task, and unknown-media libraries are rejected, while a connected known-idle supported library is allowed.

#### Objective

Prevent regressions that could expose a scan action before task state is reliable or for an unsupported Library type.

### `ServerTaskRepositoryBehaviorTest`

<details>
<summary>Paths</summary>

```text
core-data/src/test/java/dev/halim/shelfdroid/core/data/task/ServerTaskRepositoryBehaviorTest.kt
```
</details>

#### What changed

Adds deterministic repository tests for active-snapshot reconciliation, terminal retention, placeholder replacement, task events arriving before HTTP responses, immediate versus later recovery snapshots, reconnect and refresh behavior without polling, result expiry, catalog synchronization failure and retry, synchronization-state preservation, and durable deduplicated notifications.

#### Objective

Exercise the reducer's concurrency and lifecycle failure surface, especially the ordering edges where HTTP acceptance, task creation, socket delivery, reconnect snapshots, and screen recreation can race.

### Server-task payload mapping tests

<details>
<summary>Paths</summary>

```text
core-data/src/test/java/dev/halim/shelfdroid/core/data/task/ServerTaskRepositoryTest.kt
```
</details>

#### What changed

Adds unit tests for mapping scan payloads into library identity and result counts, preserving active state until explicit completion, distinguishing failure from cancellation, retaining safe server messages, and redacting internal-looking errors.

#### Objective

Verify that both HTTP snapshots and socket payloads produce stable domain state and user-safe error data.

### `LibraryAdministrationViewModel`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationViewModel.kt
```
</details>

#### What changed

Collects application-scoped task state and notifications, refreshes task state alongside libraries, and derives per-library task state from connection reliability and active/latest tasks. Adds scan and synchronization-retry events, gates scan dispatch through UI-state rules, acknowledges consumed notifications, and translates infrastructure failures into generic localized error keys rather than exposing exception messages.

#### Objective

Connect durable task tracking to lifecycle-aware UI state while ensuring scans cannot start from stale state and terminal feedback survives ViewModel recreation until the UI consumes it.

### `LibraryAdministrationItem`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationItem.kt
```
</details>

#### What changed

Adds an accessible tonal scan button with a tooltip and enablement supplied by state. Library rows now display active or terminal scan status, result counts, elapsed time, safe or generic task errors, synchronization failure, and a retry action that targets the completed task.

#### Objective

Give administrators a direct scan control and enough durable task feedback to understand progress, completion, failure, cancellation, and post-scan catalog recovery.

### `LibraryAdministrationScreen`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationScreen.kt
```
</details>

#### What changed

Routes scan and synchronization-retry events from each row, passes the relevant task and derived scan eligibility into the item, and renders localized scan/synchronization errors. It also presents terminal task notifications as one-time snackbars and acknowledges them after display.

#### Objective

Integrate task control and feedback into the existing Library administration screen while keeping durable notification ownership outside the composable lifecycle.

### Library scan strings

<details>
<summary>Paths</summary>

```text
core-ui/src/main/res/values/strings.xml
```
</details>

#### What changed

Adds localized resources for the scan action, active and terminal statuses, result counts, elapsed time, generic start/task errors, synchronization failure, and synchronization retry.

#### Objective

Provide accessible, localizable user-facing text for every new scan state and keep internal failure messages out of the UI.

### `LibraryAdministrationContentTest`

<details>
<summary>Paths</summary>

```text
core-ui/src/androidTest/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationContentTest.kt
```
</details>

#### What changed

Extends Compose UI coverage to verify scan buttons are enabled only for known-idle tasks, completed task details and synchronization retry are rendered and actionable, and generic failures resolve through localized resources.

#### Objective

Validate the user-visible scan gate, terminal result presentation, recovery action, accessibility, and safe-error rendering at the composable boundary.

### `LibraryAdministrationViewModelTest`

<details>
<summary>Paths</summary>

```text
core-ui/src/test/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationViewModelTest.kt
```
</details>

#### What changed

Adds a task-aware contract fake and tests that scans start only from a connected known-idle state, active tasks suppress repeat requests, scan and synchronization failures become generic UI errors, and terminal notifications remain available across ViewModel recreation until acknowledged. Existing formatting-only changes around long test expressions do not affect the documented behavior.

#### Objective

Protect the presentation-layer contract between durable repository state and one-shot user actions, including failure redaction and lifecycle continuity.

### Library scan implementation ticket

<details>
<summary>Paths</summary>

```text
docs/scratch/library-administration/07-scan-library-task.md
```
</details>

#### What changed

Adds the completed local issue describing the scan action, asynchronous task semantics, application-scoped tracking, reconnect recovery, safety gates, retained results, notifications, safe errors, synchronization retry, and required tests.

#### Objective

Record the delivered feature boundary and acceptance criteria in the repository's local issue tracker.
