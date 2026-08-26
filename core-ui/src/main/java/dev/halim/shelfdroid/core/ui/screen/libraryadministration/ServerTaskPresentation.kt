package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.annotation.StringRes
import dev.halim.shelfdroid.core.data.task.ServerTaskAction
import dev.halim.shelfdroid.core.data.task.ServerTaskStatus
import dev.halim.shelfdroid.core.ui.R

/** The known result shape used when rendering a Server task. */
internal enum class ServerTaskPresentationKind {
  LIBRARY_SCAN,
  BOOK_MATCHING,
  UNKNOWN,
}

/** One action/status mapping shared by task rows and terminal notifications. */
internal data class ServerTaskPresentation(
  val kind: ServerTaskPresentationKind,
  @StringRes val statusLabel: Int,
  @StringRes val countsLabel: Int? = null,
  @StringRes val elapsedLabel: Int? = null,
)

internal fun serverTaskPresentation(
  action: ServerTaskAction?,
  status: ServerTaskStatus,
): ServerTaskPresentation {
  val kind =
    when (action) {
      ServerTaskAction.LibraryScan -> ServerTaskPresentationKind.LIBRARY_SCAN
      ServerTaskAction.BookMatching -> ServerTaskPresentationKind.BOOK_MATCHING
      is ServerTaskAction.Unknown,
      null -> ServerTaskPresentationKind.UNKNOWN
    }
  return when (kind) {
    ServerTaskPresentationKind.LIBRARY_SCAN ->
      ServerTaskPresentation(
        kind = kind,
        statusLabel =
          when (status) {
            ServerTaskStatus.ACTIVE -> R.string.library_scan_active
            ServerTaskStatus.COMPLETED -> R.string.library_scan_completed
            ServerTaskStatus.FAILED -> R.string.library_scan_failed
            ServerTaskStatus.CANCELLED -> R.string.library_scan_cancelled
          },
        countsLabel = R.string.library_scan_counts,
        elapsedLabel = R.string.library_scan_elapsed,
      )
    ServerTaskPresentationKind.BOOK_MATCHING ->
      ServerTaskPresentation(
        kind = kind,
        statusLabel =
          when (status) {
            ServerTaskStatus.ACTIVE -> R.string.library_match_active
            ServerTaskStatus.COMPLETED -> R.string.library_match_completed
            ServerTaskStatus.FAILED -> R.string.library_match_failed
            ServerTaskStatus.CANCELLED -> R.string.library_match_cancelled
          },
        countsLabel = R.string.library_match_counts,
        elapsedLabel = R.string.library_match_elapsed,
      )
    ServerTaskPresentationKind.UNKNOWN ->
      ServerTaskPresentation(
        kind = kind,
        statusLabel =
          when (status) {
            ServerTaskStatus.ACTIVE -> R.string.library_task_active
            ServerTaskStatus.COMPLETED -> R.string.library_task_completed
            ServerTaskStatus.FAILED -> R.string.library_task_failed
            ServerTaskStatus.CANCELLED -> R.string.library_task_cancelled
          },
      )
  }
}
