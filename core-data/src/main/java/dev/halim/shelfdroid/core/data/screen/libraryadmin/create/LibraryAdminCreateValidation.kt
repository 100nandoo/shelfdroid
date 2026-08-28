package dev.halim.shelfdroid.core.data.screen.libraryadmin.create

import dev.halim.shelfdroid.core.MediaType
import java.util.Locale

enum class LibraryAdminCreateField {
  NAME,
  MEDIA_TYPE,
  ICON,
  PROVIDER,
  FOLDERS,
  SETTINGS_FINISH_THRESHOLD,
  SCANNER_PRECEDENCE,
  SCHEDULE,
}

enum class LibraryAdminCreateError {
  NAME_REQUIRED,
  MEDIA_TYPE_REQUIRED,
  PROVIDER_REQUIRED,
  PROVIDER_UNAVAILABLE,
  FOLDERS_REQUIRED,
  DUPLICATE_FOLDER,
  OVERLAPPING_FOLDER,
  INVALID_FINISH_THRESHOLD,
  SCANNER_PRECEDENCE_REQUIRED,
  SCHEDULE_REQUIRED,
  SCHEDULE_INVALID,
  SCHEDULE_VALIDATION_UNAVAILABLE,
}

data class LibraryAdminValidation(
  val errors: Map<LibraryAdminCreateField, List<LibraryAdminCreateError>> = emptyMap()
) {
  val isValid: Boolean
    get() = errors.isEmpty()

  val firstInvalidField: LibraryAdminCreateField?
    get() = errors.keys.firstOrNull()
}

/**
 * Validates only values the client can validate safely. In particular, a folder is not required to
 * exist: Audiobookshelf creates missing directories when the library is submitted.
 */
fun validateLibraryAdminDraft(
  draft: LibraryAdminDraft,
  providers: List<LibraryAdminProvider>?,
): LibraryAdminValidation {
  val errors = linkedMapOf<LibraryAdminCreateField, MutableList<LibraryAdminCreateError>>()
  fun add(field: LibraryAdminCreateField, error: LibraryAdminCreateError) {
    errors.getOrPut(field) { mutableListOf() }.add(error)
  }

  if (draft.name.trim().isBlank()) {
    add(LibraryAdminCreateField.NAME, LibraryAdminCreateError.NAME_REQUIRED)
  }
  if (draft.mediaType == MediaType.UNKNOWN) {
    add(LibraryAdminCreateField.MEDIA_TYPE, LibraryAdminCreateError.MEDIA_TYPE_REQUIRED)
  }
  if (providers == null) {
    add(LibraryAdminCreateField.PROVIDER, LibraryAdminCreateError.PROVIDER_UNAVAILABLE)
  } else if (providers.isEmpty() || draft.provider.isNullOrBlank()) {
    add(LibraryAdminCreateField.PROVIDER, LibraryAdminCreateError.PROVIDER_REQUIRED)
  } else if (providers.none { it.id == draft.provider }) {
    add(LibraryAdminCreateField.PROVIDER, LibraryAdminCreateError.PROVIDER_UNAVAILABLE)
  }

  val normalizedFolders = draft.folders.map(::normalizeLibraryFolderPath)
  if (normalizedFolders.isEmpty()) {
    add(LibraryAdminCreateField.FOLDERS, LibraryAdminCreateError.FOLDERS_REQUIRED)
  } else {
    val duplicate =
      normalizedFolders.groupingBy { it.comparisonKey() }.eachCount().any { it.value > 1 }
    if (duplicate) {
      add(LibraryAdminCreateField.FOLDERS, LibraryAdminCreateError.DUPLICATE_FOLDER)
    }
    if (
      normalizedFolders.indices.any { index ->
        normalizedFolders.indices.drop(index + 1).any { other ->
          normalizedFolders[index].isSameOrParentOf(normalizedFolders[other]) ||
            normalizedFolders[other].isSameOrParentOf(normalizedFolders[index])
        }
      }
    ) {
      add(LibraryAdminCreateField.FOLDERS, LibraryAdminCreateError.OVERLAPPING_FOLDER)
    }
  }

  val finishPercent: Int?
  val finishTimeRemaining: Int?
  if (draft.mediaType == MediaType.PODCAST) {
    finishPercent = draft.podcastSettings.markAsFinishedPercentComplete
    finishTimeRemaining = draft.podcastSettings.markAsFinishedTimeRemaining
  } else {
    finishPercent = draft.bookSettings.markAsFinishedPercentComplete
    finishTimeRemaining = draft.bookSettings.markAsFinishedTimeRemaining
  }
  if (
    finishPercent != null && finishPercent !in 0..100 ||
      finishTimeRemaining != null && finishTimeRemaining < 0
  ) {
    add(
      LibraryAdminCreateField.SETTINGS_FINISH_THRESHOLD,
      LibraryAdminCreateError.INVALID_FINISH_THRESHOLD,
    )
  }
  if (draft.mediaType == MediaType.BOOK && draft.metadataSources.none { it.enabled }) {
    add(
      LibraryAdminCreateField.SCANNER_PRECEDENCE,
      LibraryAdminCreateError.SCANNER_PRECEDENCE_REQUIRED,
    )
  }

  if (draft.schedule.enabled) {
    when (draft.schedule.localValidationMessage()) {
      null -> Unit
      "Enter a five-field cron expression." ->
        add(LibraryAdminCreateField.SCHEDULE, LibraryAdminCreateError.SCHEDULE_REQUIRED)
      else -> add(LibraryAdminCreateField.SCHEDULE, LibraryAdminCreateError.SCHEDULE_INVALID)
    }
  }

  return LibraryAdminValidation(errors.mapValues { it.value.toList() })
}

/** Normalizes separators without imposing POSIX rules on Windows drive paths. */
fun normalizeLibraryFolderPath(path: String): String {
  val trimmed = path.trim().replace('\\', '/')
  if (trimmed.isEmpty()) return ""
  val prefix = if (trimmed.startsWith("//")) "//" else if (trimmed.startsWith('/')) "/" else ""
  val withoutPrefix = trimmed.removePrefix("//").removePrefix("/")
  val segments = withoutPrefix.split('/').filter { it.isNotEmpty() && it != "." }
  val result = (prefix + segments.joinToString("/"))
  return when {
    result == "/" || result == "//" -> result
    result.matches(Regex("^[A-Za-z]:$")) -> "$result/"
    else -> result.trimEnd('/')
  }
}

private fun String.comparisonKey(): String =
  if (isWindowsLibraryPath(this)) lowercase(Locale.ROOT) else this

private fun String.isSameOrParentOf(other: String): Boolean {
  val left = comparisonKey().trimEnd('/')
  val right = other.comparisonKey().trimEnd('/')
  return left == right || right.startsWith("$left/")
}

private fun isWindowsLibraryPath(path: String): Boolean =
  path.matches(Regex("^[A-Za-z]:/.*")) || path.startsWith("//")
