# Split Library Administration From Core UI

## Goal

Move the Library administration screens and ViewModels from `:core-ui` into one Android library module, `:feature-library-admin`. Keep the shared Compose elements in `:core-ui`, the Library administration contracts and server task ownership in `:core-data`, and the root navigation host in `:app`. Measure whether edits to this feature stop recompiling `:core-ui` and improve incremental build time.

This plan follows [Separate Navigation Contracts From Composition](navigation-contracts-and-composition.md). Its prerequisite is already present: `:navigation-api` owns the sealed destination keys and `:app` owns `MainNavigation` and `MainActivity`. This is a regular Android library included in the app, not a Play Feature Delivery module.

## Current state and seam

- The source slice is the 13 Kotlin files under `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/libraryadmin/`. It includes the Library list, create/edit flow, server-folder browser, four settings tabs, task presentation, and two Hilt ViewModels. The two largest ViewModels are the feature's implementation and stay with its screens.
- The list consumes `LibraryAdminContract` and task state from `:core-data`. The create/edit flow consumes `LibraryAdminCreateContract` from `:core-data`. Do not move those contracts or the application-scoped server task repository; ADR 0014 requires task progress to survive navigation and coexist with other consumers.
- The feature imports shared components and preview helpers from `:core-ui`. `:app` imports the two screen entry points for navigation; check for any other production callers before moving the files.
- `app/.../navigation/Navigation.kt` registers `Libraries`, `CreateLibrary`, and `EditLibrary`. It passes navigation callbacks, sends `LibraryChangedNavResult` after a successful save, and pops the editor. `LibraryAdminScreen` consumes that result to refresh.
- Three unit tests and two Compose instrumentation tests are under the matching `core-ui/src/test/` and `core-ui/src/androidTest/` packages. The current screenshot suite has no Library administration wrapper; ADR 0002 keeps screenshot testing in `:core-ui` for now.
- Feature code currently imports `dev.halim.shelfdroid.core.ui.R`. It uses many Library administration strings and library icon drawables alongside shared strings and icons. With non-transitive R classes, resource ownership must be explicit after the move.

## Target dependencies

```text
:app ─────────────► :feature-library-admin ─────► :core-ui (shared UI)
  │                         ├───────────────────► :core-data
  │                         ├───────────────────► :core
  │                         └───────────────────► :navigation-api
  └───────────────► :core-ui, :navigation-api, :core-data (existing)
```

`:core-ui` must never depend on `:feature-library-admin`. Use `implementation` for project dependencies unless a type appears in the module's public interface. Keep the feature's public surface to its navigation entry builder and any screen entry points required by the host; make helpers and presentation types `internal` or `private` where their callers allow it.

## Step 0: Record a comparable baseline

1. With the current module layout, warm the Gradle daemon and dependency cache. Record `:app:assembleDebug` with an up-to-date tree, then with repeatable non-ABI edits to `LibraryAdminCreateViewModel.kt` and `LibraryAdminCreateScreen.kt`. Keep the same edits available to replay after the split and restore each file after measuring.
2. Use the existing timing method in `navigation-contracts-and-composition-benchmark.md`: offline dependencies, configuration cache, `--profile`, one warm-up followed by five runs per scenario. Record wall time and the `:core-ui:compileDebugKotlin` and `:app:compileDebugKotlin` task times. Also record one clean build for an overhead check.
3. Put the raw runs and medians in `docs/plan/library-administration-module-split-benchmark.md`. The earlier Book-screen benchmark is context, not a baseline for this feature.

## Step 1: Create the feature module

1. Add `:feature-library-admin` to `settings.gradle.kts` and a `featureLibraryAdmin = ":feature-library-admin"` project-path alias in `gradle/libs.versions.toml`.
2. Create `feature-library-admin/build.gradle.kts` as an Android library with its own namespace, Compose compiler, Hilt Gradle plugin, KSP, Java/Kotlin 17, and the project's current SDK settings. Do not apply the screenshot plugin. Add direct dependencies for `:core`, `:core-data`, `:core-ui`, `:navigation-api`, Compose Material 3/UI, lifecycle Compose/ViewModel, Hilt, Navigation 3, and the test libraries actually used by the moved sources. Keep this list smaller than a copy of `core-ui/build.gradle.kts`.
3. Add `implementation(project(libs.versions.featureLibraryAdmin.get()))` to `:app`. Do not add that dependency to `:core-ui` or `:core-data`.

**Gate:** Gradle sync and `:feature-library-admin:compileDebugKotlin` succeed before moving navigation registration. A temporary empty module is acceptable at this gate.

## Step 2: Move implementation and tests

1. Move all 13 production Kotlin files and the three matching unit tests into the feature module. Move `LibraryAdminContentTest.kt` and `LibraryAdminCreateContentTest.kt` into its `src/androidTest/` tree. Preserve Kotlin package names during this step to reduce call-site churn. The tests must move with `internal` declarations so their visibility remains valid.
2. Keep Hilt ViewModels and their assisted factory in the feature module. Verify that the app's Hilt aggregation sees both ViewModels and that `LibraryAdminCreateScreen` still constructs the assisted ViewModel with the `libraryId` argument.
3. Keep the nested `NavDisplay` for the create/edit details and server-folder browser inside the feature. It is part of the Library administration flow, distinct from the app's root navigation host.
4. Add `AndroidJUnitRunner` and the required Compose UI test dependencies in the new module; move test dependencies out of `:core-ui` only if nothing remaining uses them.

**Gate:** `:feature-library-admin:testDebugUnitTest`, `:feature-library-admin:compileDebugAndroidTestKotlin`, and `:app:assembleDebug` pass. A fresh source search finds no production references to the moved implementation from `:core-ui`.

## Step 3: Give feature resources a clear owner

1. Inventory every `R.string` and `R.drawable` reference in the moved files. Move Library administration-only strings and `library_icon_*` drawables from `core-ui/src/main/res/` into `feature-library-admin/src/main/res/`. Check every candidate for references in other Kotlin and XML resources before moving it. Leave broadly shared strings (`retry`, `save`, `cancel`, etc.) and generic icons in `:core-ui`.
2. Change feature sources to import the new module's `R` for owned resources and alias `dev.halim.shelfdroid.core.ui.R` only for the shared resources that remain. Remove migrated definitions from `:core-ui` so there is one owner per resource name. Update tests that compare resource IDs.
3. Check compilation with `android.nonTransitiveRClass=true`, the merged resources, and the UI tests. Do not move screenshot reference images or add feature screenshots in this split; changing screenshot ownership would require revisiting ADR 0002.

**Gate:** `:feature-library-admin:assembleDebug`, `:app:assembleDebug`, and both moved instrumentation test classes compile. A resource or string edit confined to the feature does not cause `:core-ui` resource processing.

## Step 4: Move the three navigation entries behind one feature interface

1. Expose one small `EntryProviderScope<ShelfNavKey>.libraryAdministrationEntries(...)` builder from the feature. It installs `Libraries`, `CreateLibrary`, and `EditLibrary` together. Pass the host's shared-transition scope plus `navigate` and `pop` callbacks; do not pass an app-owned `ShelfNavigator` type into the feature.
2. Move the existing `Nav3ScreenWrapper` from its private app location into shared `:core-ui` UI code so both the app and feature can use the same transition locals. The feature entry builder uses it for all three entries. Keep the root `NavDisplay`, back stack, and other destination registration in `:app`.
3. In `app/.../navigation/Navigation.kt`, replace the three inline entries with one `libraryAdministrationEntries(...)` call. Inside the feature builder, preserve `ResultEffect<LibraryChangedNavResult>` on the list, and preserve `sendResult` followed by `pop` after create/edit success. Keep the `Libraries`, `CreateLibrary`, and `EditLibrary` key classes in `:navigation-api`; their sealed serialization setup is unchanged.
4. Avoid Hilt multibinding for a single extracted feature. The app invokes the entry builder directly.

**Gate:** Create, edit, and return to list still refresh through the result bus; system back and the editor's unsaved-change handling still work; the root and nested back stacks retain state after activity recreation.

## Step 5: Verify behavior and build impact

1. Run `:feature-library-admin:testDebugUnitTest`, `:core-ui:testDebugUnitTest`, `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:core-ui:validateDebugScreenshotTest`. Run `:feature-library-admin:connectedDebugAndroidTest` on an emulator or device for the two moved UI test classes.
2. Manually verify Library list load/refresh, create and edit for Book and Podcast libraries, folder selection and overlap warning, schedule validation, reorder/delete, Library scan and Book matching progress after navigating away and back, and create/edit result refresh.
3. Replay the Step 0 edit scenarios. The structural success condition is that a non-ABI edit in `:feature-library-admin` no longer executes `:core-ui:compileDebugKotlin`; record whether the app compilation is avoided and compare median wall times. Compare the clean build as well. Do not attribute a timing difference to the split if the runs differ in daemon warmth, cache state, or work performed.
4. If the first split improves isolation but adds material clean-build or configuration overhead, record that tradeoff before extracting another feature. Retain the module only if its interface and measured behavior justify the added Gradle configuration.

## Suggested commit sequence

1. `build: add library administration feature module`
2. `refactor: move library administration screens and tests`
3. `refactor: move library administration resources`
4. `refactor: register library administration navigation from feature`
5. `docs: record library administration build timings`

## References

- [Android modularization patterns](https://developer.android.com/topic/modularization/patterns)
- [Android Navigation 3 modularization](https://developer.android.com/guide/navigation/navigation-3/modularize)
- [ADR 0002: screenshot testing in core UI](../adr/0002-compose-preview-screenshot-testing-in-core-ui.md)
- [ADR 0014: shared socket lifecycle for server tasks](../adr/0014-shared-socket-lifecycle-for-server-tasks.md)
