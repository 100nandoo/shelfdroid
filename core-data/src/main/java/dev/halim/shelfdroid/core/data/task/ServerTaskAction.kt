package dev.halim.shelfdroid.core.data.task

/**
 * The operation represented by an Audiobookshelf Server task.
 *
 * The server can add actions without a corresponding ShelfDroid release. Keeping the wire value on
 * [Unknown] lets the operation-agnostic task repository retain those tasks instead of dropping or
 * misclassifying them.
 */
sealed interface ServerTaskAction {
  val rawValue: String

  /** Alias used when the value is being forwarded back to a server-facing boundary. */
  val raw: String
    get() = rawValue

  data object LibraryScan : ServerTaskAction {
    override val rawValue: String = "library-scan"
  }

  data object BookMatching : ServerTaskAction {
    override val rawValue: String = "library-match-all"
  }

  data class Unknown(override val rawValue: String) : ServerTaskAction

  companion object {
    fun fromRaw(rawValue: String): ServerTaskAction =
      when (rawValue) {
        LibraryScan.rawValue -> LibraryScan
        BookMatching.rawValue -> BookMatching
        else -> Unknown(rawValue)
      }

    fun parse(rawValue: String): ServerTaskAction = fromRaw(rawValue)
  }
}
