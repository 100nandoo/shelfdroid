package dev.halim.shelfdroid.download.storage.podcast

internal class PodcastFolderSelectionPolicy {

  fun resolveRelativePath(
    exactRelativePath: String,
    filename: String,
    matches: Collection<String>,
  ): String? {
    if (filename.isBlank()) return null
    if (matches.any { it == exactRelativePath }) {
      return exactRelativePath
    }

    val distinctMatches = matches.distinct()
    return distinctMatches.singleOrNull()
  }
}
