# Navigation Contracts Build Timing

Measured on 2026-09-21 on the same machine with a warm Gradle daemon, populated dependency cache, offline dependency resolution, configuration cache, and `--profile`. Command: `./gradlew :app:assembleDebug --offline --console=plain --profile`. The screen edit changed only a Kotlin comment immediately before `BookScreen`, with a different comment for each run. The source was restored after each series. Each series has five runs following an initial warm-up build.

| Scenario | Before wall times (s) | Before median | After wall times (s) | After median |
| --- | --- | ---: | --- | ---: |
| Up to date | 2.95, 0.80, 0.81, 0.83, 0.95 | 0.83s | 2.88, 0.92, 0.87, 0.93, 0.85 | 0.92s |
| Book screen comment edit | 10.48, 3.29, 3.07, 2.84, 2.67 | 3.07s | 7.03, 2.41, 2.26, 2.19, 2.30 | 2.30s |

The Gradle profile reported 0s for project configuration on the warm configuration-cache runs. The first post-change up-to-date run rebuilt configuration and reported 0.585s. For the screen edits, median `:core-ui:compileDebugKotlin` task time was 1.042s before and 0.772s after; median `:app:compileDebugKotlin` task time was 0.004s in both series. The first edit in each series was substantially slower than later edits. These figures are a local comparison only; the contracts move does not itself establish a general build-speed improvement.

The structural result is that an extracted feature can depend on `:navigation-api` and `:core-ui` while its host remains in `:app`. Feature extraction and a repeat of the edit benchmark are separate follow-up work.
