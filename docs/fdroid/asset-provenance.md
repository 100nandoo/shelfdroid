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

The current listing screenshot set is a repo-tracked collection of listing-only PNGs. These files are not packaged in the APK.

Commit history:
- phone screenshots were refreshed in `6f878e34` (`docs(fdroid): update descripton and screenshots`)
- tablet screenshots were added in `3005b66d` (`docs(fdroid): add tablet screenshots`)

ShelfDroid also keeps project-owned preview fixture data in [`core-ui/src/main/java/dev/halim/shelfdroid/core/ui/preview/Defaults.kt`](../../core-ui/src/main/java/dev/halim/shelfdroid/core/ui/preview/Defaults.kt) and Compose screenshot-test references in [`core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt`](../../core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt). Those remain useful when regenerating listing assets, but the PNGs under `fastlane/metadata/android/en-US/images/` are the actual review targets and should be treated as curated listing exports rather than byte-for-byte copies of the screenshot-test references.

Phone listing assets:

| Asset | UI area | Dimensions | SHA-256 | Added in commit |
| --- | --- | --- | --- | --- |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/01-home-book.png` | home, books | `1080x2400` | `0864b5002116ded58c643d4c33d13c549ba6e5ff3bc5c6ad5a1a37abb129b9bd` | `6f878e34` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/02-home-podcast.png` | home, podcasts | `1080x2400` | `c4a5b11dace24967aac0ccdf7f149b7347d9a23b35175ecb2e18a2f7490651fc` | `6f878e34` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/03-book.png` | book detail | `1080x2400` | `60441615131fdd0375291ca0b1d9cfc0e83850df8cdb2041b4d3ffa2abb7bddc` | `6f878e34` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/04-podcast.png` | podcast detail | `1080x2400` | `41720ba818f5d6669d848e1bfcb6070508d82547cd75f35b66a9cc9eba6113ee` | `6f878e34` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/05-episode.png` | episode detail | `1080x2400` | `6d65f0babaf3f95bf9b4f139d0df3594c5c8ec6fde9c9e3cba8cf94a8a712e6b` | `6f878e34` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/06-player-book.png` | book player | `1080x2400` | `a8dd89c1ec959720e3e06814bdfddd7428517a2c0530929f4079b8c9b2ce882a` | `6f878e34` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/07-player-podcast.png` | podcast player | `1080x2400` | `bc98c8e8f734030da6e1e03060634bfd2b3b50f7c4c5a3e64b6fe286b0706d88` | `6f878e34` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/08-settings.png` | settings | `1080x2400` | `4b08f91e02edd077777ea23237104772c4b5fbe9ce0f06280f43d2640d486abd` | `6f878e34` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/09-backups.png` | backups | `1080x2400` | `1dd6ecc05fa2806ceb7dbe56a5082532055ed5d40198fd076b89c6802b8051d9` | `6f878e34` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/10-server-settings.png` | server settings | `1080x2400` | `d2ffacbb8f9d38ad4721628e7f5741de105cf365782c7f054cd9e155b803b86c` | `6f878e34` |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/11-edit-book.png` | edit book | `1080x2400` | `ffa670dbb6168d2c1909e69953b2d432f1de7cf83ba8c39fa4fade8214976a2f` | `6f878e34` |

Seven-inch tablet listing assets:

| Asset | UI area | Dimensions | SHA-256 | Added in commit |
| --- | --- | --- | --- | --- |
| `fastlane/metadata/android/en-US/images/sevenInchScreenshots/01-home-podcast.png` | home, podcasts | `1920x1200` | `b743f0a97c37c8666d9dc0f118338469cbf5f75ef6dd93b638deb85370e6b2f3` | `3005b66d` |
| `fastlane/metadata/android/en-US/images/sevenInchScreenshots/02-settings.png` | settings | `1920x1200` | `6bf1de94c2f6db5383d380dfbfbfba626aa4e4f34d04ba4d2f765c97bc6b0d6b` | `3005b66d` |

Ten-inch tablet listing assets:

| Asset | UI area | Dimensions | SHA-256 | Added in commit |
| --- | --- | --- | --- | --- |
| `fastlane/metadata/android/en-US/images/tenInchScreenshots/01-home-book.png` | home, books | `1200x1920` | `e0e015a9e088c0a4348ab4c955da654c7a70c61a48a40dbcadc9350e58a0f7cb` | `3005b66d` |
| `fastlane/metadata/android/en-US/images/tenInchScreenshots/02-book.png` | book detail | `1200x1920` | `0acbebcb2d8e59c7170074369ae4a963fa26098e2e57bdd88f2a2ac1c4d7fad6` | `3005b66d` |

Screenshot safety notes:
- preview defaults intentionally keep `IMAGE_URL = ""` and `BOOK_COVER = ""` in [`core-ui/src/main/java/dev/halim/shelfdroid/core/ui/preview/Defaults.kt`](../../core-ui/src/main/java/dev/halim/shelfdroid/core/ui/preview/Defaults.kt), so preview-derived listing assets render ShelfDroid's own fallback cover UI instead of third-party cover artwork
- the fastlane PNGs are the authoritative listing assets for F-Droid review, even when a corresponding Compose preview or screenshot-test reference also exists elsewhere in the repo

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

If you refresh a screenshot that does not come directly from the current screenshot-test reference set, capture it from a build seeded only with project-owned or placeholder data, then update the dimensions and hashes recorded here.

## Current conclusion

For the current release line, no bundled font or F-Droid listing asset needs replacement:
- bundled fonts are traceable to open-licensed upstream font projects
- launcher and listing icon assets are project-authored originals
- listing screenshots are repo-tracked review assets with pinned hashes and documented commit provenance
