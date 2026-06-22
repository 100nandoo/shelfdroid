# F-Droid Asset Provenance

This document records the exact bundled fonts and visual assets used by ShelfDroid's current F-Droid main repository submission path.

Scope:
- assets shipped in the APK release line
- listing-only assets under `fastlane/metadata/android/en-US/images/`

Non-goals:
- adding an in-app legal screen
- documenting every non-visual source file in the repo

## Runtime fonts

These three variable font binaries are bundled in the APK and wired through [`core-ui/src/main/java/dev/halim/shelfdroid/core/ui/theme/Font.kt`](../../core-ui/src/main/java/dev/halim/shelfdroid/core/ui/theme/Font.kt). They were added in commit `708e495e` (`feat(Font): remove downloadable font, use local font instead`).

| Asset | UI use | Upstream source | Redistribution basis | SHA-256 |
| --- | --- | --- | --- | --- |
| `core-ui/src/main/res/font/inter_variable.ttf` | body text | `https://github.com/rsms/inter` | SIL Open Font License 1.1: `https://raw.githubusercontent.com/rsms/inter/master/LICENSE.txt` | `29160a80ff49ddcab2c97711247e08b1fab27a484a329ce8b813d820dc559031` |
| `core-ui/src/main/res/font/lora_variable.ttf` | display, headline, title text | `https://github.com/cyrealtype/Lora-Cyrillic` | SIL Open Font License 1.1: `https://raw.githubusercontent.com/cyrealtype/Lora-Cyrillic/master/OFL.txt` | `822a6621ccbe8d97d20ac88c1c41f5615c9c2c202eaa75f272cd452aac6475a7` |
| `core-ui/src/main/res/font/jetbrains_mono_variable.ttf` | label text | `https://github.com/JetBrains/JetBrainsMono` | SIL Open Font License 1.1: `https://raw.githubusercontent.com/JetBrains/JetBrainsMono/master/OFL.txt` | `3cfafa86e28b87184d592fef82846e8c10cb48653c62efcda34f082da225ec34` |

Local metadata check:
- `fc-scan` reports the embedded families as `Inter`, `Lora`, and `JetBrains Mono`.
- The binaries are repo-local and replace the previous downloadable-font setup, so the release line does not depend on runtime font fetching.

## Runtime launcher assets

ShelfDroid's launcher mark is project-authored artwork added in commit `5b3f1192` (`feat(Launcher-Icon): change launcher icon`). The source assets live in the repo and the packaged Android launcher resources derive from them.

Source assets:
- `resources/icon_only.svg`
  - SHA-256: `0d5bd5af899c5f30e864c7bd65952e3e969330a01bc29b26b9c86f6d317fd4b1`
- `resources/launcher.svg`
  - SHA-256: `d7b82b236292e9ced0907cc2675800f665ef4a7a2949a13303dcb23b9c3ba26e`

Packaged launcher resources:
- [`app/src/main/res/drawable/ic_launcher_foreground.xml`](../../app/src/main/res/drawable/ic_launcher_foreground.xml) contains the same bar-shape mark as `resources/icon_only.svg`.
- [`app/src/main/res/values/ic_launcher_background.xml`](../../app/src/main/res/values/ic_launcher_background.xml) defines the launcher background color `#FFC981`.
- [`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`](../../app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml) and the generated `mipmap-*` rasters package that same in-repo design for Android launchers.

Redistribution basis:
- these icon assets are original project artwork and are distributed with the rest of the repository under ShelfDroid's AGPL-3.0-or-later license
- no third-party icon pack, stock illustration, or proprietary brand asset is bundled for the launcher

## Listing icon

The F-Droid listing icon is a byte-for-byte copy of the repo's 512x512 marketing icon export.

| Asset | Provenance | Dimensions | SHA-256 |
| --- | --- | --- | --- |
| `app/src/main/ic_launcher-playstore.png` | raster export of the repo-authored launcher artwork added in `5b3f1192` | `512x512` | `47ffb2f8cefac76ce73853f7be217d5a3116a1d6a1be74dc812cf458e0e74ccd` |
| `fastlane/metadata/android/en-US/images/icon.png` | direct copy of `app/src/main/ic_launcher-playstore.png`, added for F-Droid metadata in `8cb7d531` | `512x512` | `47ffb2f8cefac76ce73853f7be217d5a3116a1d6a1be74dc812cf458e0e74ccd` |

## Listing screenshots

The five F-Droid phone screenshots are direct copies of git-tracked Compose screenshot-test references. They were added to `fastlane` in commit `8cb7d531` and were not edited after copy.

| Listing asset | Direct source file | Screenshot source | SHA-256 |
| --- | --- | --- | --- |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/01-home.png` | `core-ui/src/screenshotTestDebug/reference/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviewsKt/HomeScreenGridScreenshot_Light_4a9950f2_0.png` | `HomeScreenGridScreenshot()` in [`core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt`](../../core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt) | `c0089f78f67e0fc0bee4005ae97b2159afda8413a9257755dd64455999a073b4` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/02-book.png` | `core-ui/src/screenshotTestDebug/reference/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviewsKt/BookScreenScreenshot_Light_4a9950f2_0.png` | `BookScreenScreenshot()` in [`core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt`](../../core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt) | `d0c88605627cc10c4e672e0de4256e1e7ff0c3a057aa6ba5bd60eec51e5c92df` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/03-podcast.png` | `core-ui/src/screenshotTestDebug/reference/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviewsKt/PodcastScreenScreenshot_Light_4a9950f2_0.png` | `PodcastScreenScreenshot()` in [`core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt`](../../core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt) | `1ec1be5523f74af2005d4a262874fb65bf5bddcc60753ea48dc84013498dca7c` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/04-episode.png` | `core-ui/src/screenshotTestDebug/reference/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviewsKt/EpisodeScreenScreenshot_Light_4a9950f2_0.png` | `EpisodeScreenScreenshot()` in [`core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt`](../../core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt) | `8b37393aacb56586c27e4572eac55fc6085f473713c1aa8411f4c11275052bbb` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/05-player.png` | `core-ui/src/screenshotTestDebug/reference/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviewsKt/BigPlayerScreenshot_Light_4a9950f2_0.png` | `BigPlayerScreenshot()` in [`core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt`](../../core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt) | `26769b76406a9443ca7d523c7d4257fec00dd208044d2c3d117859e149673fa8` |

Screenshot safety notes:
- all five listing screenshots are `1440x3120`
- the direct sources live under `core-ui/src/screenshotTestDebug/reference/`, so reviewers can diff or regenerate them from source
- the generating previews are declared in [`core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt`](../../core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt)
- the preview titles, descriptions, and other visible copy for these screenshots are project-owned placeholder fixture data defined in [`core-ui/src/main/java/dev/halim/shelfdroid/core/ui/preview/Defaults.kt`](../../core-ui/src/main/java/dev/halim/shelfdroid/core/ui/preview/Defaults.kt)
- preview defaults intentionally keep `IMAGE_URL = ""` and `BOOK_COVER = ""` in [`core-ui/src/main/java/dev/halim/shelfdroid/core/ui/preview/Defaults.kt`](../../core-ui/src/main/java/dev/halim/shelfdroid/core/ui/preview/Defaults.kt), so the screenshots render ShelfDroid's own fallback cover UI instead of bundling third-party cover artwork

## How to re-verify or refresh

Validate the checked-in screenshot references:

```bash
./gradlew :core-ui:validateDebugScreenshotTest
```

Refresh the checked-in screenshot references after an intentional UI change:

```bash
./gradlew :core-ui:updateDebugScreenshotTest
```

If the F-Droid listing screenshots are refreshed, copy the updated light-mode reference PNGs from `core-ui/src/screenshotTestDebug/reference/.../CoreUiScreenshotTestPreviewsKt/` into `fastlane/metadata/android/en-US/images/phoneScreenshots/` without additional edits, then update the hashes in this file.

## Current conclusion

For the current release line, no bundled font or F-Droid listing asset needs replacement:
- bundled fonts are traceable to open-licensed upstream font projects
- launcher and listing icon assets are project-authored originals
- listing screenshots are reproducible repo-owned outputs that use project-owned placeholder copy and do not embed third-party cover artwork
