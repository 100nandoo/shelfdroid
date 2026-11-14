# Backup/Restore Coverage and Benchmark Diagnostics

Lifecycle: proposed
Updated: 2026-11-14

Status: planned. Created 2026-11-14 following the November 5 dependency updates.

## Dependency context

- Android Gradle Plugin (AGP) 9.5.0 → 9.5.1: no publicly documented fixes in the patch. Experimental automated backup/restore testing is available in the 9.5 release family.
- UI Automator 2.4.0 → 2.4.1: fixes ShellServer port collisions. The modern test scope, element queries, stability helpers, screenshots, and ResultsReporter were already available in 2.4.0.

These plans use capabilities already available at the repository's current versions; neither requires another dependency upgrade.

## 1. Backup and restore coverage

### Objective

Verify that restored ShelfDroid data is usable and that missing device-local resources do not leave the app in a broken state.

### Current state

- The app manifest enables backup and references backup rules.
- `app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml` contain template rules without an explicit application data policy.
- Backup behavior must respect the shared-storage Download decision in ADR 0001 and session recovery/logout behavior in ADR 0004.

### Work

1. Inventory persisted data and its storage locations: preferences, database, session credentials, Download records, media files, and cached state. Check manifest wiring for both legacy backup and Android 12+ extraction rules.
2. Define expected restore behavior for each category before changing rules. Decide which settings survive, how sessions recover or require login, and how missing Download files are reconciled. Do not assume external media files transfer with app data.
3. Configure an isolated test suite using AGP's experimental backup testing support. Verify the current DSL and device/transport requirements with `android docs` before implementation. The documented flags are `android.experimental.testSuiteSupport=true` and `android.experimental.enableBackupTest=true`; suites use `testOptions.backupTests`.
4. Seed deterministic preferences and representative database records, perform backup/restore, then launch the app and assert the agreed behavior.
5. Cover preferences, database usability, expired or unavailable sessions, and Download records whose files are absent. Verify that restoring local data does not overwrite newer server Progress when synchronization resumes.
6. Start with a reproducible supported local restore scenario. Add cloud and device-to-device scenarios where the test environment supports them, recording prerequisites and limitations.
7. Document the command, required device setup, fixture preparation, and HTML report location. Add CI execution only after the local suite is reliable.

### Acceptance criteria

- A real backup/restore cycle verifies intended settings and database state through app behavior.
- Invalid sessions recover or request login according to ADR 0004 without crashes or retry loops.
- Missing media files do not appear as usable Downloads or cause playback crashes.
- Fixtures are isolated from real accounts and media; test reports do not expose credentials.
- Unsupported transports are explicitly identified, rather than reported as passing coverage.

## 2. Better benchmark diagnostics

### Objective

Make login and Catalog navigation failures actionable while preserving the meaning of startup and frame timing measurements.

### Current state

`benchmark/src/main/java/dev/halim/benchmark/ExampleStartupBenchmark.kt` measures authenticated cold launch and frame timing. It uses `By` selectors and a custom `requireObject` helper with a 10-second timeout. Scenario arguments supply the Audiobookshelf server, credentials, and destination text.

### Work

1. Review UI Automator 2.4 APIs with `android docs`. Keep existing selectors where they are clear; adopt the modern scope and built-in waits where they simplify interactions.
2. Add failure context for the scenario phase, expected element, package, and timeout. Distinguish server-field, login, Catalog pager, and destination failures.
3. Capture a screenshot and UI hierarchy on failure, using ResultsReporter where appropriate. Use distinct artifact names per test/iteration and document their location.
4. Prevent password values and session tokens from leaking into logs or artifacts. Avoid captures while credentials are visible unless sensitive fields are reliably excluded or redacted.
5. Use app visibility/stability helpers only at justified synchronization points. Keep diagnostic capture on failure and avoid adding arbitrary waits to the measured block.
6. Preserve cold-start mode, metrics, iteration count, and authenticated scenario. If synchronization changes the measured workload, document it and establish a new baseline.

### Acceptance criteria

- Successful startup and frame timing runs still produce normal benchmark results.
- Deliberately missing selectors and an unsuccessful login produce phase-specific failure messages and useful, safe artifacts.
- Missing instrumentation arguments still fail clearly before attempting the scenario.
- No unconditional screenshot capture or diagnostic delay is added to successful measured iterations.
- The documented local command identifies required arguments without embedding real credentials.

## Suggested order

Implement backup/restore coverage first, then benchmark diagnostics. Each can be delivered independently; keep production behavior changes limited to issues demonstrated by the restore tests.

## References

- [AGP 9.5 release notes](https://developer.android.com/build/releases/agp-9-5-0-release-notes)
- [Automated backup and restore testing](https://developer.android.com/studio/preview/backup-restore-testing)
- [UI Automator release notes](https://developer.android.com/jetpack/androidx/releases/test-uiautomator)
