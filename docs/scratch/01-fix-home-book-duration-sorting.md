# Fix Home book Duration sorting

Status: `ready-for-human`

## Problem

`HomeMapper.toBookUiState` does not copy a Book's duration into `BookUiState`, so every cached Book reaches the Home sorting comparator with the default duration of zero. Selecting Duration therefore does not meaningfully order Books.

## Scope

- Map the cached Book duration into `BookUiState`.
- Store Catalog durations as raw numeric seconds and rebuild the Catalog cache destructively on upgrade.
- Add focused tests for ascending and descending Duration ordering on Home.
- Keep this repair separate from author sorting.

The destructive migration clears only the Catalog-related tables (`LibraryItemEntity`, `BookEntity`,
`PodcastEntity`, and `PodcastEpisodeEntity`). Listening progress, bookmarks, preferences, and other
unrelated local state are preserved. A user who upgrades while offline sees an empty Catalog until
the next successful synchronization.

When durations compare equal, Home preserves the order returned by SQLDelight; no additional
application-level tie-breaker is introduced.

## Acceptance criteria

- Home orders Books by their actual duration when Duration is selected.
- Ascending and descending directions are covered by tests.
- Books with equal durations preserve the order returned by SQLDelight.
