# Separate Navigation Contracts From Composition

## Goal

Put serializable destination keys in a small module that future feature modules can depend on. Move the navigation host and Android entry activity to `:app`, so `:core-ui` can later become shared UI without depending on feature implementations. Preserve route types, saved back stacks, navigation results, deep links, and player launch behavior.

This is an enabling refactor. It does not by itself remove screens from `:core-ui` or promise a faster incremental build.

## Current seams

- `:core-ui` contains 202 production Kotlin files, including 136 screen files. `navigation/Navigation.kt` is a 521-line host that maps every `ShelfNavKey` to a screen.
- `navigation/ShelfNavKey.kt` defines the sealed, `@Serializable` key family. Its payloads use types already owned by `:core` (`AuthPromptReason` and the navigation payload types).
- `navigation/ShelfNavigator.kt` owns the `NavBackStack`, Compose remember helpers, and authentication restore policy. It is host state, not a destination contract.
- `navigation/PendingMediaIdHandler.kt`, `NavigationResultHandler.kt`, and `AppriseNotificationRuleResultMapper.kt` coordinate host behavior and results.
- `screen/MainActivity.kt` currently lives in `:core-ui`. `:app` declares it in `app/src/main/AndroidManifest.xml`, and `app/auth/OpenIdCallbackActivity.kt` launches it.
- `core-ui/Composable.kt` casts the current context to `MainActivity` to initialize the media controller when Book or Podcast screens resume. This reference must be removed before the activity can move to `:app`.
- ADR 0002 keeps screenshot tests in `:core-ui`. This plan leaves screenshot coverage and its references there.

## Target dependency direction

```text
:app ─────────► :core-ui ─────────► :navigation-api ─────────► :core
  └────────────────────────────────► :navigation-api
:core-ui ──────────────────────────────────────────────────────► :core
```

The arrows point from a consumer to a dependency: `:navigation-api` depends on `:core`; `:core-ui` depends on both; `:app` depends on `:core-ui` and `:navigation-api`. After feature extraction, `:app` also depends on feature implementations, while features depend on `:navigation-api` and shared UI. No library module depends on `:app`.

Before Step 1, record the current `:app:assembleDebug` timing for an up-to-date build and a repeatable non-ABI edit in one `:core-ui` screen. Use the same scenarios again in Step 4.

## Step 1: Extract the destination contracts

1. Add `:navigation-api` to `settings.gradle.kts` and a `navigation-api` entry in `gradle/libs.versions.toml`, following the existing project-path alias pattern.
2. Create `navigation-api/build.gradle.kts` as a minimal Android library. Apply the Android library and Kotlin serialization plugins. Use the existing Java/Kotlin 17 and SDK settings. Depend on `:core`, Navigation 3 runtime, and kotlinx serialization. Expose dependencies that appear in the public key types to consumers; do not add Hilt, KSP, Compose UI, or screenshot testing.
3. Move `core-ui/.../navigation/ShelfNavKey.kt` into the new module. Keep the Kotlin package and every key name, field, default value, and `@Serializable` annotation unchanged. Keep the sealed interface and all of its direct implementations together so the current serializer and restored back stacks retain the same type names.
4. Add a direct dependency from `:core-ui` to `:navigation-api`. Keep `ShelfNavigator.kt`, `Navigation.kt`, result handling, and `MainActivity.kt` where they are in this step. Compile and run the existing `:core-ui` navigation tests before proceeding.

**Gate:** `:navigation-api` has no dependency on `:core-ui`, `:app`, media, download, or screen implementations. `:app:assembleDebug` and `:core-ui:testDebugUnitTest` pass. Add a focused serialization round-trip test for a plain key and a key with a `:core` payload; keep existing route serialization names intact.

## Step 2: Remove the activity reference from shared UI

1. Replace `InitMediaControllerIfMainActivity()` in `core-ui/Composable.kt` with a screen-scoped lifecycle callback supplied by the navigation host. Update Book and Podcast screen parameters/callers so the media controller still initializes on resume for those screens, without testing the context against `MainActivity`.
2. Verify those screens do not initialize the media controller at unrelated destinations and still work after returning from the background.
3. Search the remaining `:core-ui` source and tests for references to `MainActivity` or app-only classes; remove them before moving the activity. Do not introduce an interface solely to preserve the cast.

**Gate:** `:core-ui` compiles without a source reference to `MainActivity`; Book and Podcast playback entry paths still initialize the controller.

## Step 3: Move composition and host behavior into `:app`

1. Move `Navigation.kt`, `ShelfNavigator.kt`, `AuthPromptNavigation.kt`, `PendingMediaIdHandler.kt`, `NavigationResultHandler.kt`, and `AppriseNotificationRuleResultMapper.kt` from `:core-ui` to `:app`. Keep their Kotlin package names during this move. Keep the `NavDisplay`, entry mappings, back stack, result bus handling, and player wrapper behavior together.
2. Move `MainActivity.kt` to `:app`, initially preserving `dev.halim.shelfdroid.core.ui.screen.MainActivity`. The existing manifest class name and `OpenIdCallbackActivity` import should therefore remain valid. Check the merged manifest rather than assuming this.
3. Give `:app` direct dependencies for the types its moved sources import: `:navigation-api`, `:download`, `:helper`, `:media`, core splash screen, Navigation 3 runtime/UI, and lifecycle ViewModel Navigation 3, alongside its existing dependencies. Remove dependencies from `:core-ui` only after checking its remaining screens and tests for imports.
4. Move the four host-logic unit test files under `core-ui/src/test/.../navigation/` to `app/src/test/.../navigation/` with their source files. Add JUnit to `:app` test dependencies. Keep screen, preview, and screenshot tests in `:core-ui`.
5. Verify the dependency graph: `:app` may import `:core-ui`, but no source in `:core-ui` may import a type supplied only by `:app`. Keep the navigation host as a single composition point for now; feature entry providers are a later step.

**Gate:** `:app:assembleDebug`, `:app:testDebugUnitTest`, `:core-ui:testDebugUnitTest`, and `:core-ui:validateDebugScreenshotTest` pass. Confirm cold login, forced re-login, logout, back and predictive back, Book/Podcast/player launch intents, OpenID callback return, library and API-key navigation results, and process-death restoration of a multi-entry back stack on a device or emulator.

## Step 4: Measure before extracting a feature

1. Repeat the pre-change build scenarios after Step 3 using the same machine, Gradle daemon state, and dependency cache. Compare medians from at least five runs after warm-up; record configuration, Kotlin compilation, and total wall time.
2. Do not claim a speedup based on the contracts move alone. The structural outcome is that an extracted feature can depend on `:navigation-api` and `:core-ui`, while the host in `:app` can depend on that feature without a cycle.
3. As a separate follow-up, extract one cohesive feature (Library administration is a candidate), move its navigation entry mapping with it, and repeat the same edit benchmark. Keep the feature split only if it improves change isolation or maintainability without a material clean-build regression.

## Decisions and limits

- Keep all `ShelfNavKey` implementations in one module for this refactor. Moving sealed subclasses into separate feature modules would require a different key hierarchy and explicit serialization/state-restoration work.
- Preserve packages during file moves to limit call-site and serialized-name churn. Package cleanup can be separate.
- Keep `:core-ui` screenshot tests in place under ADR 0002. If a later feature extraction moves a covered screen, update the ADR and screenshot ownership together.
- Do not add Hilt multibinding for entry registration until multiple feature modules exist. Direct registration is sufficient for the first feature.

## References

- [Android Navigation 3 modularization](https://developer.android.com/guide/navigation/navigation-3/modularize)
- [Android Navigation 3 modular Hilt recipe](https://developer.android.com/guide/navigation/navigation-3/recipes/modular-hilt)
- [ADR 0002: Compose preview screenshot testing in core UI](../adr/0002-compose-preview-screenshot-testing-in-core-ui.md)
