package dev.halim.shelfdroid.core.data.screen.libraryadministration

import java.util.Locale

enum class LibraryAdministrationCreateField {
  NAME,
  MEDIA_TYPE,
  ICON,
  PROVIDER,
  FOLDERS,
  SETTINGS_FINISH_THRESHOLD,
  SCANNER_PRECEDENCE,
}

enum class LibraryAdministrationCreateError {
  NAME_REQUIRED,
  MEDIA_TYPE_REQUIRED,
  PROVIDER_REQUIRED,
  PROVIDER_UNAVAILABLE,
  FOLDERS_REQUIRED,
  DUPLICATE_FOLDER,
  OVERLAPPING_FOLDER,
  INVALID_FINISH_THRESHOLD,
  SCANNER_PRECEDENCE_REQUIRED,
}

data class LibraryAdministrationValidation(
  val errors: Map<LibraryAdministrationCreateField, List<LibraryAdministrationCreateError>> =
    emptyMap()
) {
  val isValid: Boolean
    get() = errors.isEmpty()

  val firstInvalidField: LibraryAdministrationCreateField?
    get() = errors.keys.firstOrNull()
}

/**
 * Validates only values the client can validate safely. In particular, a folder is not required to
 * exist: Audiobookshelf creates missing directories when the library is submitted.
 */
fun validateLibraryAdministrationDraft(
  draft: LibraryAdministrationDraft,
  providers: List<LibraryAdministrationProvider>?,
): LibraryAdministrationValidation {
  val errors = linkedMapOf<LibraryAdministrationCreateField, MutableList<LibraryAdministrationCreateError>>()
  fun add(field: LibraryAdministrationCreateField, error: LibraryAdministrationCreateError) {
    errors.getOrPut(field) { mutableListOf() }.add(error)
  }

  if (draft.name.trim().isBlank()) {
    add(LibraryAdministrationCreateField.NAME, LibraryAdministrationCreateError.NAME_REQUIRED)
  }
  if (draft.mediaType == LibraryAdministrationMediaType.UNKNOWN) {
    add(LibraryAdministrationCreateField.MEDIA_TYPE, LibraryAdministrationCreateError.MEDIA_TYPE_REQUIRED)
  }
  if (providers == null) {
    add(LibraryAdministrationCreateField.PROVIDER, LibraryAdministrationCreateError.PROVIDER_UNAVAILABLE)
  } else if (providers.isEmpty() || draft.provider.isNullOrBlank()) {
    add(LibraryAdministrationCreateField.PROVIDER, LibraryAdministrationCreateError.PROVIDER_REQUIRED)
  } else if (providers.none { it.id == draft.provider }) {
    add(LibraryAdministrationCreateField.PROVIDER, LibraryAdministrationCreateError.PROVIDER_UNAVAILABLE)
  }

  val normalizedFolders = draft.folders.map(::normalizeLibraryFolderPath)
  if (normalizedFolders.isEmpty()) {
    add(LibraryAdministrationCreateField.FOLDERS, LibraryAdministrationCreateError.FOLDERS_REQUIRED)
  } else {
    val duplicate = normalizedFolders.groupingBy { it.comparisonKey() }.eachCount().any { it.value > 1 }
    if (duplicate) {
      add(LibraryAdministrationCreateField.FOLDERS, LibraryAdministrationCreateError.DUPLICATE_FOLDER)
    }
    if (normalizedFolders.indices.any { index ->
      normalizedFolders.indices.drop(index + 1).any { other ->
        normalizedFolders[index].isSameOrParentOf(normalizedFolders[other]) ||
          normalizedFolders[other].isSameOrParentOf(normalizedFolders[index])
      }
    }) {
      add(LibraryAdministrationCreateField.FOLDERS, LibraryAdministrationCreateError.OVERLAPPING_FOLDER)
    }
  }

  val finishPercent: Int?
  val finishTimeRemaining: Int?
  if (draft.mediaType == LibraryAdministrationMediaType.PODCAST) {
    finishPercent = draft.podcastSettings.markAsFinishedPercentComplete
    finishTimeRemaining = draft.podcastSettings.markAsFinishedTimeRemaining
  } else {
    finishPercent = draft.bookSettings.markAsFinishedPercentComplete
    finishTimeRemaining = draft.bookSettings.markAsFinishedTimeRemaining
  }
  if (finishPercent != null && finishPercent !in 0..100 ||
      finishTimeRemaining != null && finishTimeRemaining < 0) {
    add(
      LibraryAdministrationCreateField.SETTINGS_FINISH_THRESHOLD,
      LibraryAdministrationCreateError.INVALID_FINISH_THRESHOLD,
    )
  }
  if (draft.mediaType == LibraryAdministrationMediaType.BOOK &&
      draft.metadataSources.none { it.enabled }) {
    add(
      LibraryAdministrationCreateField.SCANNER_PRECEDENCE,
      LibraryAdministrationCreateError.SCANNER_PRECEDENCE_REQUIRED,
    )
  }

  return LibraryAdministrationValidation(errors.mapValues { it.value.toList() })
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
