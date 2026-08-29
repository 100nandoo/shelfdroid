## Commit

**SHA:** `db100ca2b00c714a17c8b9830ce7de46b8b3a927`

**Title:** `feat(libraries): add scan scheduling`

## Commit Objective

Add automatic scan scheduling to the Library creation flow. The commit introduces website-equivalent simple presets and an advanced cron mode, validates schedules locally and through Audiobookshelf where appropriate, shows a readable schedule and next run, and serializes the active cron expression with the create request.

It also prevents creation while schedule validation is pending or unsuccessful and preserves the user's separate simple and advanced drafts when switching modes.

## Substantive Changes

### `LibraryAdministrationScheduleDraft`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationCreateModel.kt
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationSchedule.kt
core-data/src/test/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationScheduleTest.kt
```
</details>

#### What changed

Adds schedule state to `LibraryAdministrationDraft` and defines the reusable scheduling model. A schedule can be disabled, use a simple interval or weekday/time configuration, or use an independently retained advanced five-field cron expression. The model converts valid simple selections into Audiobookshelf cron expressions, supplies summaries and local validation messages, omits the expression when scheduling is disabled, and calculates a readable next occurrence for supported standard cron syntax.

Unit tests cover disabled and default schedules, weekday/time conversion, all interval presets, preservation of both mode drafts, five-field advanced validation, and next-run calculation.

#### Objective

Keep schedule intent in the create draft as a single serializable domain value while preventing hidden state from one mode from leaking into another. The local conversion, description, and next-run helpers let the UI explain a schedule before it is submitted and catch simple input errors without a network call.

### `LibraryAdministrationScheduleValidationState`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationCreateContract.kt
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationCreateUiState.kt
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationCreateValidation.kt
```
</details>

#### What changed

Extends the create contract with schedule validation and distinguishes a server-rejected expression from an unavailable validation service. The create state gains a Schedule tab, asynchronous validation states, Schedule-specific field and error identifiers, and an `isBusy` guard that combines submission and validation work. Draft validation now requires a valid local schedule whenever automatic scans are enabled.

#### Objective

Give the data and UI layers a shared contract for representing schedule validation, focusing the Schedule tab after an error, and preventing submission during either schedule checking or Library creation. Separating invalid input from service unavailability also allows the UI to present actionable failure messages.

### `LibraryAdministrationRepository`

<details>
<summary>Paths</summary>

```text
core-data/src/main/java/dev/halim/shelfdroid/core/data/screen/libraryadministration/LibraryAdministrationRepository.kt
core-network/src/main/java/dev/halim/core/network/request/CreateLibraryRequest.kt
core-network/src/test/java/dev/halim/core/network/LibraryAdministrationApiServiceTest.kt
```
</details>

#### What changed

Implements schedule validation by posting a `ValidateCronRequest` through `ApiService`; HTTP 400 responses become invalid-schedule failures, while other failures become validation-unavailable failures. Library creation now maps the active schedule expression to the `autoScanCronExpression` setting for both book and podcast Libraries, leaving it null when scheduling is disabled.

Network tests verify the enabled schedule field in the serialized create payload and confirm that cron validation posts the expression to `/api/validate-cron`.

#### Objective

Use Audiobookshelf as the semantic authority for advanced cron expressions and carry an accepted schedule into the same create request as the Library's other settings. The error mapping preserves the distinction between bad input and a retryable connectivity or server problem.

### `LibraryAdministrationScheduleContent`

<details>
<summary>Paths</summary>

```text
core-ui/src/androidTest/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationCreateContentTest.kt
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationCreateScreen.kt
core-ui/src/main/res/values/strings.xml
```
</details>

#### What changed

Adds the Schedule tab and its localized UI: an enable switch, simple/advanced mode chips, interval presets, weekday chips, hour and minute inputs, an advanced cron field and validation action, inline accessible errors, a schedule summary, and a calculated next run. The create and validation actions respect the combined busy state, and Schedule errors receive their own focus target.

Tab indexing now derives from the visible tab list so the Schedule tab remains correctly selected for podcast Libraries, where Scanner is hidden. An instrumented Compose test covers that podcast tab edge and confirms the Schedule content renders while Scanner remains absent.

#### Objective

Expose the complete scheduling workflow in the Library form while keeping validation status and errors understandable and accessible. Computing the selected index from visible tabs prevents the new fourth tab from selecting the wrong tab when a media-type-specific tab is omitted.

### `LibraryAdministrationCreateViewModel`

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationCreateViewModel.kt
core-ui/src/test/java/dev/halim/shelfdroid/core/ui/screen/libraryadministration/LibraryAdministrationCreateViewModelTest.kt
```
</details>

#### What changed

Adds events and reducers for enabling schedules, switching modes and intervals, editing time and weekdays, updating advanced cron, and requesting explicit validation. Schedule edits clear prior validation so an accepted result cannot be reused after the draft changes. Submission locally validates all modes, creates simple schedules without a server round trip, and validates advanced schedules before creation unless the current draft already has a valid result.

The asynchronous flow ignores results for a changed draft, maps invalid and unavailable failures into Schedule errors, focuses the Schedule tab, and blocks duplicate validation or create requests while work is in flight. Tests exercise server rejection, validation unavailability, explicit validation without creation, duplicate-submit protection during validation, and direct creation with a simple preset.

#### Objective

Coordinate schedule editing and server validation as part of the existing create state machine so only the active, currently validated advanced expression can reach Library creation. The busy and stale-result guards reduce duplicate mutations and prevent delayed validation responses from applying to newer form state.

### Library scan schedule ticket

<details>
<summary>Paths</summary>

```text
docs/scratch/library-administration/05-library-scan-schedule.md
```
</details>

#### What changed

Adds the completed local issue record for automatic Library scan scheduling, including acceptance checks for disabled and preset schedules, weekday/time and interval controls, advanced server validation, readable feedback, stale-mode isolation, and success and failure coverage.

#### Objective

Record the feature boundary, dependency, completion status, and verification expectations alongside the implementation.
