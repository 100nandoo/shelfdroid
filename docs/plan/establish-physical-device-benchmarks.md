# Establish usable physical-device benchmarks

Status: Implemented; measurement blocked without a physical device
Priority: Medium

## Evidence

Both `ExampleStartupBenchmark.startup` and `frameTiming` failed the `EMULATOR` environment
check on 2026-09-16. No valid performance measurements were collected, so this audit cannot
establish a performance improvement or regression.

The benchmark source also uses empty login inputs, nullable UI lookups, and a wait for
`Main` whose result is not asserted. Those assumptions must be validated before treating
measurements as a reliable performance baseline.

## Plan

1. Select a dedicated physical Android device and record its model, OS, refresh rate,
   power/thermal conditions, and benchmark build configuration.
2. Define the intended scenarios precisely: cold launch to login, or cold launch to a seeded
   catalog. Make authentication and fixture setup deterministic for the chosen scenario.
3. Update obsolete UI selectors and assert that setup and destination waits succeed.
   A benchmark must fail if it measures an unintended screen.
4. Run `./gradlew :benchmark:connectedBenchmarkAndroidTest` with only the intended physical
   device connected. Keep the emulator guard enabled.
5. Capture repeated baseline measurements and traces for a recorded commit. Measure future
   changes on the same device and scenario; choose regression tolerances from observed
   variability rather than inventing a threshold before data exists.

## Implementation

The benchmark now measures a cold launch from a clean, authenticated app state into a
configured Catalog destination. Each iteration clears the target app data, launches the app,
grants the app's audio permission, performs password sign-in with non-blank instrumentation
arguments, and asserts that the configured destination is reached before measuring. UI Automator
selectors are scoped to the target package and use the current Compose test tags (`server`,
`Username`, `Password`, `Login`, the stable `catalog` pager marker, and the
`catalog-library` page marker). The benchmark device
must use the English app locale because the three login tags are currently derived from localized
strings. The Macrobenchmark library's `EMULATOR` guard remains enabled; no benchmark error is
suppressed.

The benchmark waits for the library page to be composed, performs one rightward pager swipe from
the initial Misc page, and requires the configured title to be a descendant of the library grid.
Use an isolated Audiobookshelf server fixture with one library and a seeded Library item whose
title is visible without scrolling. Reset that server to the same versioned snapshot before each
baseline; the benchmark only resets the client app data and cannot make a live remote Catalog
immutable.

`StartupTimingMetric` reports the app's first display and its full-display signal when Home data
has finished synchronizing. The subsequent measured journey opens the library page and asserts the
seeded Library item; treat startup and frame-timing distributions as separate endpoints rather
than assuming the pager swipe changes startup timing.

Run on the dedicated physical device with credentials kept outside the repository:

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.benchmark.server="$SHELFDROID_BENCHMARK_SERVER" \
  -Pandroid.testInstrumentationRunnerArguments.benchmark.username="$SHELFDROID_BENCHMARK_USERNAME" \
  -Pandroid.testInstrumentationRunnerArguments.benchmark.password="$SHELFDROID_BENCHMARK_PASSWORD" \
  -Pandroid.testInstrumentationRunnerArguments.benchmark.destination="$SHELFDROID_BENCHMARK_DESTINATION"
```

`benchmark.destination` must be an exact, visible title from the seeded Catalog. The benchmark
variant is non-debuggable and minified through the existing `app` build configuration. Gradle
copies JSON measurement reports and Perfetto traces under
`benchmark/build/outputs/connected_android_test_additional_output/benchmarkAndroidTest/connected/`.

Record the following with each baseline: commit (`git rev-parse HEAD`), device model, Android
release/API level, display refresh rate, screen brightness, battery/charging state, thermal state,
server fixture/catalog identifier, benchmark variant, and the exact command arguments except for
the password. Keep repeated startup and frame-timing distributions and traces together; do not
replace them with a single timing comment.

The current workspace has only the `emulator-5554` device connected, so physical measurement and
baseline artifact capture remain blocked. Emulator runs must continue to fail the built-in guard.

## Validation and acceptance

- Both benchmark cases complete on the physical device and emit measurement artifacts.
- Setup assertions prove the intended screen is reached in every iteration.
- Baseline artifacts identify the commit, device, fixture, and conditions.
- Compare startup and frame-timing distributions over repeated runs before claiming an
  improvement or regression. Historical timing comments alone are not a comparable baseline.
- If no physical device is available, mark measurement execution as blocked; do not suppress
  the emulator guard and present emulator timings as representative performance results.
