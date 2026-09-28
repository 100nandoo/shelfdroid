## Commit

**SHA:** `1fd23853210cc368cd16fa49192bb101c6881cea`

**Title:** `refactor(navigation): standardize Nav3 result events`

## Commit Objective

Use Navigation 3 `ResultEventBus` consistently for results returned by child destinations. Make each receiver handle its typed event directly and clear the event after processing, while removing obsolete argument based result handling and unused result key constants.

## Substantive Changes

### Result event receivers

<details>
<summary>Paths</summary>

```text
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/apikeys/ApiKeysScreen.kt
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/apprisenotificationsettings/AppriseNotificationSettingsScreen.kt
core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/searchpodcast/SearchPodcastScreen.kt
feature-library-admin/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadmin/LibraryAdminScreen.kt
```
</details>

#### What changed

Each screen now listens for its result type with `ResultEffect`, handles the result in its existing ViewModel flow, and removes the consumed result from the bus. Search podcast and API key screens also drop their unused argument based result parameters. The navigation-only `collectNavResultEvent` switches are removed from the receiver APIs.

#### Objective

Keep one-time navigation results on the Navigation 3 event path and prevent handled events from being delivered again if a receiver re-enters composition.

### Navigation entry wiring

<details>
<summary>Paths</summary>

```text
app/src/main/java/dev/halim/shelfdroid/core/ui/navigation/Navigation.kt
feature-library-admin/src/main/java/dev/halim/shelfdroid/feature/library/admin/navigation/LibraryAdministrationEntries.kt
```
</details>

#### What changed

The destination entries stop passing `collectNavResultEvent` flags to result receivers. Result producers continue sending typed events through `LocalResultEventBus` before applying their navigation changes.

#### Objective

Make result listening part of the receiving screen’s normal behavior whenever it is hosted under the result bus decorator, while keeping result sending alongside navigation orchestration.

### `NavResultKey`

<details>
<summary>Paths</summary>

```text
core/src/main/java/dev/halim/shelfdroid/core/navigation/NavResult.kt
```
</details>

#### What changed

Removed the unused string constants for result names.

#### Objective

Keep the result contract aligned with the type-derived keys used by all current senders and receivers.

## Verification

Passed `:app:testDebugUnitTest`, `:core-ui:testDebugUnitTest`, and `:feature-library-admin:testDebugUnitTest`.
