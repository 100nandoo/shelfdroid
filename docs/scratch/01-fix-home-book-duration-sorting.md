# Fix Home book Duration sorting

Status: `ready-for-agent`

## Problem

`HomeMapper.toBookUiState` does not copy a Book's duration into `BookUiState`, so every cached Book reaches the Home sorting comparator with the default duration of zero. Selecting Duration therefore does not meaningfully order Books.

## Scope

- Map the cached Book duration into `BookUiState`.
- Add focused tests for ascending and descending Duration ordering on Home.
- Keep this repair separate from author sorting.

## Acceptance criteria

- Home orders Books by their actual duration when Duration is selected.
- Ascending and descending directions are covered by tests.
- Books with equal durations have deterministic ordering.
