# Library administration

## Objective

Add administrator-only Library administration to ShelfDroid with behavioural parity with the
Audiobookshelf 2.36.0 website, expressed as native Material 3 Android UI.

## In scope

- Load and refresh the server's ordered Libraries.
- Create Book and Podcast Libraries through bottom-aligned scrollable tabs.
- Configure Library details, applicable settings, book scanner precedence, and scan schedules.
- Browse server folders or enter paths manually.
- Reorder Libraries by drag and accessible move actions.
- Start Library scans and Book matching and track their Server tasks.
- Delete Libraries with clear media-retention messaging.
- Reconcile mutations and external Library events with the local catalog.
- Cover each slice with functional, state, failure, and accessibility tests.

## Behaviour

- Only admin/root users can enter Library administration.
- A failed Library request makes the screen unavailable; cached Library names are not substituted.
- Tapping a Library row has no effect. Scan, Match, and Delete are direct tonal icon buttons with
  tooltips and content descriptions. Podcast Libraries do not expose Match.
- Create uses a full-width action above bottom scrollable tabs. Podcast drafts omit Scanner.
- The create draft preserves hidden media-specific values when its media type changes, but only
  fields applicable to the final media type are submitted.
- Provider-loading failures are visible and retryable and prevent submission. Invalid submission
  selects and focuses the first invalid tab field and presents accessible inline errors.
- Manual folder paths may not exist yet because the server creates them. Blank, duplicate, and
  parent/child-overlapping paths are rejected before submission.
- Create, delete, and reorder mutations are globally serialized. Scan and Match are mutually
  exclusive per Library and may run independently for different Libraries.
- Task-sensitive actions are disabled while task state is unknown, while the socket is
  disconnected, or while that Library has an active task. Reorder and Delete are disabled for a
  Library with an active task.
- Server tasks bootstrap from HTTP and reconcile socket events without polling. Reconnection and
  explicit refresh reload the task snapshot.
- Completion exposes completed, failed, or cancelled status plus available counts and elapsed time.
  It triggers one snackbar and remains visible for one minute.
- A successful Server task followed by failed local synchronization is reported distinctly and can
  be retried.
- Delete confirmation states that catalog data is removed while media files remain. Deleting the
  active Library selects the next available one and does not forcibly stop buffered playback.
- Safe server validation details may be shown; internal server details use localized generic errors.

## Architecture boundaries

- Server-task state belongs to an application-scoped, operation-agnostic repository.
- The shared socket supports independent subscriptions and shared connection ownership.
- Rich Library administration configuration remains server-backed. Only catalog-relevant
  `displayOrder` is added to local Library persistence.
- Create form sections and state are designed for reuse by the future Edit Library phase.

## Excluded from this phase

- Edit Library.
- Existing-Library Tools tab.
- Force rescan.
- Task cancellation and a generic task-cancellation API.
- Global task/activity centre.
- Adaptive screenshot reference coverage.

## Completion

All ten tickets are complete and their agreed functional tests pass. Deferred capabilities are not
required for completion.
