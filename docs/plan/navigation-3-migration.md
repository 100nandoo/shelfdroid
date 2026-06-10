# Navigation 3 Migration Plan

## Summary

Migrate the current single-stack `navigation-compose 2.9.8` graph in `core-ui` to Navigation 3 as a big-bang replacement branch on the stable Nav 3 track. Preserve current behavior, keep `compileSdk 36`, and include Nav 3 transition and predictive-back support, but do not take the `navigation3 1.2 alpha` SDK bump. The app stays a single back stack; no adaptive or multi-stack redesign is part of this migration.

## Version Assumptions

- `androidx.navigation3:navigation3-runtime = 1.1.2`
- `androidx.navigation3:navigation3-ui = 1.1.2`
- `androidx.lifecycle:lifecycle-viewmodel-navigation3 = 2.11.0-rc01`
- Shared lifecycle catalog should move from `2.10.0` to a compatible `2.11.x` line
- `compileSdk` remains `36`

## Implementation Changes

### Dependencies and modules

- Add `androidx.navigation3:navigation3-runtime:1.1.2` and `androidx.navigation3:navigation3-ui:1.1.2` to `:core-ui`.
- Add `androidx.lifecycle:lifecycle-viewmodel-navigation3:2.11.0-rc01` to `:core-ui`.
- Add Nav 3 runtime to `:core` so cross-module navigation payloads can implement `NavKey`.
- Remove `androidx.navigation:navigation-compose` after the graph is fully migrated and no Nav 2 APIs remain.
- Keep the existing Hilt Compose dependency because the Nav 3 Hilt recipe still uses `hiltViewModel()`.

### Navigation model

- Convert every destination key used by `Navigation.kt` to `@Serializable ... : NavKey`.
- Keep the app as `NavBackStack<NavKey>` with a small `ShelfNavigator` wrapper in `core-ui/navigation`.
- Replace `rememberNavController()` / `NavHost` / `composable<T>` with:
  - `rememberNavBackStack(startKey)`
  - `NavDisplay`
  - `entryProvider { entry<T> { key -> ... } }`
- Migrate all 26 destinations in the existing graph in one pass.

### Back stack behavior

- Replace `NavHostController` mutations with explicit back-stack mutations in `ShelfNavigator`:
  - `navigate(key)` for normal forward navigation
  - `pop()` / `popToRoot()` for back flows
  - `replaceStack(listOf(...))` for login success and notification-driven jumps
- Preserve current stack semantics:
  - app start: `Login(false)` or `Home(false)` depending on token
  - login success: replace stack with `Home(true)`
  - re-login from settings: push `Login(true)`
  - media notification / open-detail intent: replace stack with `Home(false) -> Book(...)` or `Home(false) -> Podcast(...) -> Episode(...)`
- Replace `PlayerHandler` destination-route string checks with type-based checks against the current `NavKey`.

### External entrypoints

- Keep `NavRequest` in `MainActivity`, but change `handlePendingMediaId()` from `NavHostController` commands to a pure resolver that produces a target key path for `ShelfNavigator`.
- Do not adopt Nav 3 alpha deep-link APIs on this branch. Current custom activity actions and extras remain the app-owned external-entry mechanism.

### Transitions and predictive back

- Keep shared-element behavior by wrapping `NavDisplay` in the existing `SharedTransitionLayout` and passing its scope into `NavDisplay`.
- Rework `SharedScreenWrapper` and animation locals so screen content reads Nav 3's animated content scope instead of relying on `composable {}`'s `AnimatedContentScope`.
- Add `transitionSpec`, `popTransitionSpec`, and `predictivePopTransitionSpec` to `NavDisplay`.
- Keep transitions conservative so they do not fight current shared-element behavior.

### ViewModels with route arguments

Only the route-bound Hilt view models need constructor changes. These 12 view models currently depend on Navigation 2 route arguments being injected into `SavedStateHandle`, so they must switch to assisted injection with the Nav 3 key:

- `LoginViewModel`
- `HomeViewModel`
- `SearchPodcastViewModel`
- `BookViewModel`
- `PodcastViewModel`
- `EpisodeViewModel`
- `AddEpisodeViewModel`
- `AddPodcastViewModel`
- `EditItemViewModel`
- `EditUserViewModel`
- `EditApiKeysViewModel`
- `UserInfoViewModel`

For each of those:

- Replace `SavedStateHandle` route decoding with `@Assisted` NavKey injection.
- Add an `@AssistedFactory`.
- Create the view model from the route entry with:

```kotlin
hiltViewModel<VM, Factory>(
  creationCallback = { factory -> factory.create(key) }
)
```

Reason:

- In Nav 3, the primary argument source is the typed `key` provided by `entry<T> { key -> ... }`.
- The official Nav 3 Hilt pattern passes that key through assisted injection instead of relying on `SavedStateHandle` route parsing.
- `rememberViewModelStoreNavEntryDecorator()` scopes each view model instance to the specific Nav 3 entry, preserving the current one-route-instance-to-one-VM-instance behavior.

Route-free view models can stay on plain `@HiltViewModel` plus `hiltViewModel()`.

### Navigation results

- Because this branch stays on stable Nav 3, replace `savedStateHandle` back-stack results with a small `rememberNavigationResultStore()` owned beside the back stack in `MainNavigation`.
- Preserve the existing two result flows exactly:
  - `SearchPodcast <- AddPodcast`
  - `ApiKeys <- CreateEditApiKeys`
- Keep the existing `NavResultKey` constants and screen-level `result` parameters so the UI contract does not change during the migration.
- Remove all `entry.savedStateHandle`, `previousBackStackEntry`, and `currentBackStackEntry` navigation plumbing from the graph.

## Why `navigation-compose` is removed

`androidx.navigation:navigation-compose` is the Navigation 2 stack:

- `NavController`
- `rememberNavController()`
- `NavHost`
- `composable`
- `NavBackStackEntry`-based route and result handling

Navigation 3 uses different artifacts and APIs:

- `androidx.navigation3:navigation3-runtime`
- `androidx.navigation3:navigation3-ui`
- `NavBackStack`
- `NavDisplay`
- `entryProvider`
- `entry<T>`

Once the app fully replaces `NavHost` and `NavController` with `NavDisplay` and `NavBackStack`, the old dependency should be removed as final cleanup.

## Test Plan

- Add unit tests for `ShelfNavigator` and external-request resolution:
  - login success replaces stack correctly
  - book notification builds `Home -> Book`
  - episode notification builds `Home -> Podcast -> Episode`
  - player-visible route detection works by key type
- Add unit tests for the custom result store:
  - create-podcast result is delivered once to `SearchPodcast`
  - API-key-changed flag is delivered once to `ApiKeys`
- Expand `test-app` instrumentation coverage beyond the current smoke test:
  - launch into login/home correctly
  - navigate `Home -> Book / Podcast / Episode / Settings`
  - re-login flow returns to the expected screen state
  - `AddPodcast` success returns the result and opens `Podcast`
  - edit API key returns changed state to `ApiKeys`
- Manual acceptance on device or emulator:
  - shared-element transitions still work for `Home↔Book`, `Home↔Podcast`, `Podcast↔Episode`
  - predictive back behaves correctly on Android 14+
  - player overlay still hides on non-player routes and reappears on player-visible routes
  - notification intents still open the correct detail/player state
  - process death restores the current Nav 3 back stack and current screen

## Assumptions

- Target version track is stable Nav 3 with `navigation3 1.1.2` and `lifecycle-viewmodel-navigation3 2.11.0-rc01`.
- Navigation remains a single-stack app for this migration.
- Current custom intent entrypoints remain authoritative; URL deep-link expansion is out of scope.
- The migration optimizes for behavior parity plus Nav 3 transitions and predictive back, not for broader navigation redesign.

## Sources

- <https://developer.android.com/guide/navigation/navigation-3/migration-guide>
- <https://developer.android.com/guide/navigation/navigation-3/get-started>
- <https://developer.android.com/guide/navigation/navigation-3/save-state>
- <https://developer.android.com/guide/navigation/navigation-3/animate-destinations>
- <https://developer.android.com/guide/navigation/navigation-3/recipes/passingarguments>
- <https://developer.android.com/jetpack/androidx/releases/lifecycle>
