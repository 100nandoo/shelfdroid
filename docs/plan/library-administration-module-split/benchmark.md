# Library Administration Module Split Build Timing

Measured on 2026-09-21 on the same machine. Each scenario used `./gradlew :app:assembleDebug --offline --configuration-cache --console=plain --profile`, one warm-up run, then five recorded runs. Each edit appended a distinct Kotlin comment to the named file and was restored after its series. This is a non-ABI source edit. The before and after runs used the same files, content change, Gradle daemon, offline dependency resolution, and configuration-cache method.

| Scenario | Before wall times (s) | Before median | After wall times (s) | After median |
| --- | --- | ---: | --- | ---: |
| Up to date | 0.885, 0.855, 0.743, 1.437, 1.124 | 0.885s | 0.891, 0.927, 0.836, 0.878, 0.851 | 0.878s |
| `LibraryAdminCreateViewModel.kt` comment edit | 8.324, 5.356, 4.463, 4.869, 3.503 | 4.869s | 2.046, 2.006, 2.768, 2.456, 1.890 | 2.046s |
| `LibraryAdminCreateScreen.kt` comment edit | 6.473, 2.981, 4.221, 3.211, 2.705 | 3.211s | 2.323, 1.623, 1.634, 2.021, 2.453 | 2.021s |

Gradle profile task timings and execution statuses for the five recorded runs:

| Scenario | `:core-ui:compileDebugKotlin` before | `:core-ui:compileDebugKotlin` after | `:feature-library-admin:compileDebugKotlin` after | `:app:compileDebugKotlin` before / after |
| --- | --- | --- | --- | --- |
| Up to date | 0.017, 0.018, 0.015, 0.020, 0.022s; all up to date | 0.014, 0.044, 0.013, 0.014, 0.014s; all up to date | 0.004, 0.004, 0.004, 0.004, 0.004s; all up to date | Median 0.004s / 0.004s; all up to date |
| ViewModel edit | 2.369, 1.321, 1.743, 1.402, 1.202s; executed 5/5 | 0.014, 0.014, 0.015, 0.027, 0.015s; up to date 5/5 | 0.487, 0.545, 0.561, 0.531, 0.459s; executed 5/5 | Median 0.009s / 0.004s; all up to date |
| Screen edit | 1.918, 1.247, 1.618, 1.136, 0.952s; executed 5/5 | 0.015, 0.014, 0.015, 0.015, 0.014s; up to date 5/5 | 0.434, 0.443, 0.363, 0.679, 1.122s; executed 5/5 | Median 0.005s / 0.004s; all up to date |

For a clean-build overhead check, one `clean :app:assembleDebug` run took 16.418s before and 7.507s after. The before profile had 129 executed tasks and 160 from-cache tasks; the after profile had 136 executed and 173 from-cache tasks. Gradle caches and warmed compiler state affect this one-run comparison, so it does not establish a clean-build speedup. The warmed up-to-date medians were essentially equal.

The structural isolation goal is met: edits to either feature Kotlin file executed the feature compiler while `:core-ui:compileDebugKotlin` stayed up to date. App compilation was already avoided before the split and remained up to date. A feature-only string edit ran `:feature-library-admin:parseDebugLocalResources` while `:core-ui:parseDebugLocalResources`, `:core-ui:generateDebugRFile`, and `:core-ui:compileDebugKotlin` stayed up to date. The resource edit was restored after the check.

The median wall time fell by 2.823s for the ViewModel edit and 1.190s for the screen edit in this local run. The module adds configuration and tasks, but the observed isolation and warmed edit timings support retaining it. Manual server-backed Library administration workflows were not measured by this benchmark.
