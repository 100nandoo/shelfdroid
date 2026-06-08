# Compose Screenshot Coverage Expansion

## Current State

`core-ui` now has phase-one Compose Preview Screenshot Testing wired through `core-ui/src/screenshotTest/` with dedicated `@PreviewTest` wrappers instead of reusing every existing Studio preview.

- Current pilot coverage: `11` surfaces
- Current reference images: `22` PNGs in `core-ui/src/screenshotTestDebug/reference/`
- Existing preview annotations in `core-ui/src/main/java`: `149`

The current matrix is intentionally small: light and dark only, with no global expansion to dynamic color, font scale, locale, RTL, or extra devices.

---

## Goal

Expand screenshot coverage without turning the suite into PNG churn. The next step is to add more deterministic surfaces before adding more preview axes.

This follows the official Android guidance for screenshot testing:

- prefer screenshots that give unique feedback
- avoid combinatorial explosion
- keep reference image growth intentional

---

## Rules For Expansion

### Keep the wrapper split

Continue adding screenshot tests only through dedicated files in `core-ui/src/screenshotTest/`.

Do **not** point `@PreviewTest` directly at every existing `@ShelfDroidPreview` function in `src/main/java`. Many of those previews are optimized for Studio browsing, not stable screenshot baselines.

### Keep the matrix fixed for now

For the next expansion waves, keep:

- device: current `@ShelfDroidPreview` device
- themes: light and dark
- dynamic color: off
- font scale: default
- locale: default
- RTL: off

### Add breadth before depth

Increase the number of covered surfaces first. Only add new axes after a broader set of high-value surfaces is stable.

### Prefer deterministic content

Favor:

- pure content composables
- fixed fake state from `Defaults`
- static dialogs, sheets, rows, and controls

Avoid or carefully wrap:

- previews that depend on network image loading unless they render a forced fallback
- previews with implicit async behavior
- previews that mix multiple scenarios into one convenience layout

### Batch changes

Add screenshot wrappers in batches of roughly `10-15` surfaces at a time. Generate references and validate after each batch.

---

## Wave 2: Recommended Next Batch

This batch should roughly double useful coverage without increasing the scenario matrix.

### Components

- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/components/Dialog.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/components/Download.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/components/DropDown.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/components/Button.kt`

### Rows And Small Surfaces

- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/book/ProgressRow.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/apikeys/ApiKeyItem.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/backups/BackupItem.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/podcast/EpisodeItem.kt`

### Sheets And Home Item Variants

- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/player/bigplayer/BookmarkSheet.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/item/HomeItemGrid.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/item/HomeItemList.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/home/item/HomeItemBottomSheet.kt`

### Why this batch

These files are high-signal because they:

- represent reusable UI building blocks
- cover lists, rows, dialogs, and sheets that are visually sensitive
- are easier to make deterministic than large Hilt-driven screens
- expand coverage faster than adding more variants to already-covered screens

---

## Wave 3: Larger Screen Coverage

Once wave 2 is stable, expand to larger and more stateful screens:

- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/settings/SettingsScreen.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/edititem/EditItemScreen.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/listeningsession/ListeningSessionScreen.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/apikeys/createedit/CreateEditApiKeysScreen.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/usersettings/edit/EditUserScreen.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/searchpodcast/SearchPodcastScreen.kt`
- `core-ui/src/main/java/dev/halim/shelfdroid/core/ui/screen/logs/LogsScreen.kt`

These screens will likely need additional curation because they more often depend on:

- sheet state
- focus behavior
- loading and failure states
- transition locals
- layout combinations that are useful in Studio but noisy in goldens

---

## When To Add New Axes

Do not add new preview axes globally. Add them only when a specific surface benefits from them.

### Good candidates for later axis expansion

- font scale:
  `LoginScreen`, `SettingsScreen`, `EpisodeItem`, text-heavy dialogs
- locale / RTL:
  item rows, settings rows, button clusters, metadata screens
- additional device sizes:
  `HomeScreen`, `PodcastScreen`, sheets, large player layouts

### Bad pattern

Do not multiply every covered surface by:

- dynamic color
- font scale
- locale
- RTL
- small device
- large device

That would create a large reference set with weak signal and high maintenance cost.

---

## File Organization

The single screenshot wrapper file should not keep growing indefinitely.

Before or during wave 2, split `core-ui/src/screenshotTest/kotlin/dev/halim/shelfdroid/core/ui/screenshot/CoreUiScreenshotTestPreviews.kt` into:

- `components/ComponentScreenshotPreviews.kt`
- `player/PlayerScreenshotPreviews.kt`
- `screen/ScreenScreenshotPreviews.kt`

This keeps ownership clearer and reduces merge churn.

---

## Expansion Workflow

For each wave:

1. Add dedicated screenshot wrappers in `core-ui/src/screenshotTest/`
2. Generate references:

```sh
./gradlew :core-ui:updateDebugScreenshotTest
```

3. Validate references:

```sh
./gradlew :core-ui:validateDebugScreenshotTest
```

4. Review the generated HTML report if needed:

`core-ui/build/reports/screenshotTest/preview/debug/index.html`

5. Check in the new files under:

`core-ui/src/screenshotTestDebug/reference/`

---

## Exit Criteria For Wave 2

Wave 2 is complete when:

- the recommended batch is covered by dedicated screenshot wrappers
- reference generation succeeds locally
- validation passes locally
- no new global axis is introduced
- the wrapper organization remains understandable

---

## Follow-Up Question For A Future Phase

After wave 2 or wave 3, revisit whether the project should:

- keep local-machine-owned baselines only
- introduce CI validation ownership
- add a second axis for a selected subset of surfaces

That should be a separate decision, not bundled into routine coverage expansion.
