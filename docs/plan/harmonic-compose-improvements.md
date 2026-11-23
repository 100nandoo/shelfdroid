# Compose improvements inspired by Harmonic

Lifecycle: active
Updated: 2026-11-16

Status: Settings search is implemented; its acceptance checks and the remaining planned work are still open.

## Goal

Adapt useful Compose patterns from Harmonic to improve ShelfDroid's settings discovery,
loading feedback, text scrolling, and tablet experience. Evaluate image-request
coordination alongside the UI work.

## Implementation order

1. Settings search: highest user-facing value.
2. Cover-image request deduplication: establish whether duplicate network work occurs before changing configuration.
3. Cached shimmer placeholders and scroll decorations: small, independent UI improvements.
4. Resizable split panes: larger adaptive-layout work, coordinated with the existing Window Manager work.

## Planned work and references

- [x] Add settings search with title/keyword matching, navigation to the matching page,
  automatic scrolling to the control, and a brief highlight. Acceptance checks remain.
  Reference: [search index](https://github.com/SimonHalvdansson/Harmonic-HN/blob/main/core/src/commonMain/kotlin/com/simon/harmonichackernews/settings/SettingsSearch.kt)
  and [row focus](https://github.com/SimonHalvdansson/Harmonic-HN/blob/main/ui/src/commonMain/kotlin/com/simon/harmonichackernews/ui/settings/SettingsSearchFocus.kt).
- [ ] Investigate concurrent cover-image request deduplication when the same cover appears in
  the catalog, mini-player, and details. Measure duplicate requests first; adapt Harmonic's
  Coil configuration to ShelfDroid's OkHttp fetcher.
  Reference: [Coil deduplication change](https://github.com/SimonHalvdansson/Harmonic-HN/commit/624c3f5b251316bc03a2f53cb2c4a49de93da181).
- [ ] Add cached shimmer loading placeholders for covers, catalog rows, and episode rows.
  Read animation progress during drawing and cache gradient brushes.
  Reference: [ShimmerBackground.kt](https://github.com/SimonHalvdansson/Harmonic-HN/blob/main/ui/src/commonMain/kotlin/com/simon/harmonichackernews/ui/common/ShimmerBackground.kt).
- [ ] Add scroll fades and a scrollbar to overflowing book descriptions, podcast notes,
  and text dialogs where they improve scroll discoverability.
  Reference: [ScrollableTextDecorations.kt](https://github.com/SimonHalvdansson/Harmonic-HN/blob/main/ui/src/commonMain/kotlin/com/simon/harmonichackernews/ui/common/ScrollableTextDecorations.kt).
- [ ] Explore resizable tablet split panes for catalog/details or episode list/player,
  with saved proportions, keyboard controls, and accessibility semantics. Coordinate with
  the Window Manager todo; adapt the pattern without Harmonic's browser-specific logic.
  Reference: [SplitPaneViewport.kt](https://github.com/SimonHalvdansson/Harmonic-HN/blob/main/ui/src/commonMain/kotlin/com/simon/harmonichackernews/ui/navigation/SplitPaneViewport.kt).

### Additional findings

- [ ] Adapt settings navigation to show categories beside the selected settings page on
  wide windows, while retaining dialogs, drafts, and scroll state when switching layouts.
  Coordinate with the split-pane work above. Estimated effort: medium–high.
  Reference: [SettingsNavigationShell.kt](https://github.com/SimonHalvdansson/Harmonic-HN/blob/main/ui/src/commonMain/kotlin/com/simon/harmonichackernews/ui/settings/SettingsNavigationShell.kt).
- [ ] Preserve per-page settings scroll positions during navigation and window resizing.
  Avoid focus-driven scroll jumps during layout changes while preserving keyboard navigation.
  Estimated effort: medium.
  Reference: [SettingsScrollHistory.kt](https://github.com/SimonHalvdansson/Harmonic-HN/blob/main/ui/src/commonMain/kotlin/com/simon/harmonichackernews/ui/settings/SettingsScrollHistory.kt).
- [ ] Explore swipe actions for removing queue entries or dismissing appropriate list items.
  Provide an equivalent accessibility action and keep dismissal offsets transient so Undo
  does not restore an item in its dismissed position. Estimated effort: low.
  Reference: [SwipeToHideStory.kt](https://github.com/SimonHalvdansson/Harmonic-HN/blob/main/ui/src/commonMain/kotlin/com/simon/harmonichackernews/ui/stories/SwipeToHideStory.kt).

## Validation and completion criteria

### Adaptive settings navigation

- Verify category selection and Back behavior in compact and wide layouts.
- Keep open dialogs, unsaved edits, and scroll positions when crossing layout breakpoints.
- Adapt the navigation pattern to ShelfDroid's destinations rather than copying Harmonic's state.

### Settings scroll restoration

- Verify independent scroll positions when leaving and returning to different settings pages.
- Check resizing and rotation with a focused control, including keyboard-driven focus scrolling.
- Verify restoration with changing content and avoid overriding an active user scroll.

### Swipe actions

- Begin with one suitable list action and retain a non-gesture way to invoke it.
- Verify one action per dismissal, Undo restoring a settled row, and stable item identity.
- Check accessibility actions, layout direction, and interaction with vertical scrolling.

### Settings search

- Index titles, keywords, and destination identifiers; exclude preference values and credentials.
- Adapt destinations to ShelfDroid's navigation and use stable setting identifiers for row targeting.
- Verify matching, empty results, conditional controls, automatic scrolling, and one-time highlighting.
- Check that navigation back and restored screen state remain usable.

### Cover-image request deduplication

- Measure simultaneous requests for a cover shown in the catalog, mini-player, and details.
- Check the installed Coil version and OkHttp fetcher support before adapting Harmonic's configuration.
- If duplicate work exists, verify request sharing, cancellation, failures, and successful retries.
- Preserve authentication and cache isolation across accounts and Audiobookshelf servers.

### Cached shimmer placeholders

- Start with a shared modifier and apply it to selected loading states.
- Keep changing animation reads in drawing and reuse gradient brushes.
- Check light/dark themes, loading-to-content transitions, and disabled animation behavior.
- Measure rendering before claiming a performance improvement.

### Scroll decorations

- Apply to overflowing descriptions, notes, and dialogs where the affordance is useful.
- Verify short content has no unnecessary decoration and long content has correct end states.
- Check large text, layout direction, and legibility without obscuring content.

### Resizable split panes

- Begin with one catalog/details flow and preserve the compact-screen navigation behavior.
- Persist settled pane proportions and support keyboard and accessibility adjustments.
- Verify resizing, rotation, state restoration, and navigation between one and two panes.
- Extract the relevant layout pattern rather than copying Harmonic's browser-specific state.

## Scope notes

ShelfDroid already has shared cover transitions and a playback-speed slider that commits
changes when dragging finishes. Those patterns do not need to be imported as part of this plan.

Source links describe reference implementations, not guaranteed drop-in replacements.
Confirm current dependencies and behavior when implementing each item. Use focused tests
and the repository's applicable build checks; device verification requires explicit authorization.

## Settings search design decisions

Agreed during design review; implementation is in place and acceptance verification remains.

- Scope the first release to Local app preferences in client Settings. Exclude server
  administration, including Server settings, Email settings, and Authentication settings.
- Open search through a search icon in Settings. Results show the setting title and category
  path. Selecting a result opens its page, scrolls to the control, and briefly highlights it.
- Use Harmonic's ranked word and prefix matching without fuzzy matching. Lowercase text,
  treat non-letter/digit characters as word separators, and require every distinct query
  word to match in any order. Blank queries return no results.
- Score each query word using its strongest match: exact title word (100), title word prefix
  (80), exact keyword or alias word (60), keyword or alias word prefix (50), or category/path
  word prefix (20). Add the strongest full-query bonus: exact title or alias (1000), title
  prefix (500), or title substring (200). Sort by descending total score.
- Use curated keywords and aliases; exclude preference values and credentials from the index.
- Hide controls the current user cannot access. Include visible disabled controls when their
  page explains how to enable them.
- Back from a selected setting restores the query, results, and results scroll position.
  Returning does not repeat the target highlight.
- A blank query shows search guidance with no results. An unmatched query shows an empty
  result message. Do not add suggestions or persist recent-search history in this release.
- Index individual preferences, excluding logout, account details, informational links, and
  Listening sessions.
- Keep dependent preferences searchable when hidden by another preference. Selecting one
  targets its prerequisite and explains what must be enabled; never change a preference
  automatically.
- Match titles and category names in the current app language, with curated keywords and
  aliases in that language. Exclude automatic translation and cross-language matching.
- Use a dedicated search screen. Focus the search field on entry and update results as the
  user types. Selecting a result dismisses the keyboard and opens the target page. System
  Back dismisses the keyboard first; leaving search returns to Settings.
- Restore the query and results scroll position across rotation and process recreation
  while search remains in the navigation stack. Clear them when search is closed.
- Highlight the target briefly with a background treatment, without flashing or pulsing,
  and identify the target for screen-reader users. Respect reduced-motion settings. Scroll
  once per selection and do not steal keyboard focus after the user starts interacting.

### Settings search acceptance checks

- Verify normalization, all-query-word matching, prefix matches, aliases, ranking bonuses,
  and deterministic ordering for tied scores. Confirm typo-only queries do not fuzzy-match.
- Verify current-language titles and paths, blank-query guidance, and unmatched-query copy.
- Verify excluded areas and sensitive values never appear in search results.
- Verify permission changes remove inaccessible results and selection rechecks availability.
- Verify visible disabled controls and hidden dependent controls lead to useful explanations
  without changing any preference automatically.
- Verify selection reaches the correct row, highlights once, and supports screen readers,
  keyboard navigation, and reduced motion without overriding subsequent user interaction.
- Verify Back restores results and their position, keyboard dismissal precedes leaving the
  search screen, recreation restores active search, and closing search clears its state.

Design interview complete. Settings search implementation is in place; acceptance verification remains.
