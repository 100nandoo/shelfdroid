package dev.halim.shelfdroid.download

internal data class BookFolderMatch(val relativePath: String, val filename: String)

internal class BookFolderSelectionPolicy {

  fun resolveRelativePath(
    exactRelativePath: String,
    filenames: Collection<String>,
    matches: Collection<BookFolderMatch>,
  ): String {
    val requestedFilenames = filenames.filter { it.isNotBlank() }.toSet()
    if (requestedFilenames.isEmpty()) return exactRelativePath

    val groupedMatches =
      matches
        .filter { it.filename in requestedFilenames }
        .groupBy(keySelector = { it.relativePath }, valueTransform = { it.filename })
        .mapValues { (_, filenamesForPath) -> filenamesForPath.toSet() }

    val exactMatches = groupedMatches[exactRelativePath]
    if (!exactMatches.isNullOrEmpty()) {
      return exactRelativePath
    }

    val fullMatches =
      groupedMatches.filterValues { matchedFilenames ->
        matchedFilenames.containsAll(requestedFilenames)
      }
    if (fullMatches.size == 1) {
      return fullMatches.keys.first()
    }

    if (groupedMatches.size == 1) {
      return groupedMatches.keys.first()
    }

    return exactRelativePath
  }
}
