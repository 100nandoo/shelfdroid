package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import androidx.annotation.StringRes
import dev.halim.shelfdroid.core.data.task.TaskAction
import dev.halim.shelfdroid.core.data.task.TaskStatus
import dev.halim.shelfdroid.core.ui.R

/** The known result shape used when rendering a Server task. */
internal enum class TaskPresentationKind {
  LIBRARY_SCAN,
  BOOK_MATCHING,
  UNKNOWN,
}

/** One action/status mapping shared by task rows and terminal notifications. */
internal data class TaskPresentation(
  val kind: TaskPresentationKind,
  @StringRes val statusLabel: Int,
  @StringRes val countsLabel: Int? = null,
  @StringRes val elapsedLabel: Int? = null,
)

internal fun taskPresentation(
  action: TaskAction?,
  status: TaskStatus,
): TaskPresentation {
  val kind =
    when (action) {
      TaskAction.LibraryScan -> TaskPresentationKind.LIBRARY_SCAN
      TaskAction.BookMatching -> TaskPresentationKind.BOOK_MATCHING
      is TaskAction.Unknown,
      null -> TaskPresentationKind.UNKNOWN
    }
  return when (kind) {
    TaskPresentationKind.LIBRARY_SCAN ->
      TaskPresentation(
        kind = kind,
        statusLabel =
          when (status) {
            TaskStatus.ACTIVE -> R.string.library_scan_active
            TaskStatus.COMPLETED -> R.string.library_scan_completed
            TaskStatus.FAILED -> R.string.library_scan_failed
            TaskStatus.CANCELLED -> R.string.library_scan_cancelled
          },
        countsLabel = R.string.library_scan_counts,
        elapsedLabel = R.string.library_scan_elapsed,
      )
    TaskPresentationKind.BOOK_MATCHING ->
      TaskPresentation(
        kind = kind,
        statusLabel =
          when (status) {
            TaskStatus.ACTIVE -> R.string.library_match_active
            TaskStatus.COMPLETED -> R.string.library_match_completed
            TaskStatus.FAILED -> R.string.library_match_failed
            TaskStatus.CANCELLED -> R.string.library_match_cancelled
          },
        countsLabel = R.string.library_match_counts,
        elapsedLabel = R.string.library_match_elapsed,
      )
    TaskPresentationKind.UNKNOWN ->
      TaskPresentation(
        kind = kind,
        statusLabel =
          when (status) {
            TaskStatus.ACTIVE -> R.string.library_task_active
            TaskStatus.COMPLETED -> R.string.library_task_completed
            TaskStatus.FAILED -> R.string.library_task_failed
            TaskStatus.CANCELLED -> R.string.library_task_cancelled
          },
      )
  }
}
