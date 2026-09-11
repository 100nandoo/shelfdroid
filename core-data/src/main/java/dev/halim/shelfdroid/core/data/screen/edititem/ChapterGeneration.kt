package dev.halim.shelfdroid.core.data.screen.edititem

data class ChapterSourceTrack(
  val filename: String,
  val duration: Double,
  val excluded: Boolean = false,
)

fun generateChaptersFromTracks(tracks: List<ChapterSourceTrack>): Result<List<ChapterRow>> {
  val includedTracks = tracks.filterNot { it.excluded }
  if (includedTracks.isEmpty()) {
    return Result.failure(IllegalArgumentException("No eligible tracks"))
  }
  if (
    includedTracks.any {
      it.filename.isBlank() || !it.duration.isFinite() || it.duration <= 0.0
    }
  ) {
    return Result.failure(IllegalArgumentException("Invalid track data"))
  }

  var currentStart = 0.0
  val chapters =
    includedTracks.mapIndexed { index, track ->
      val title = track.filename.fileNameWithoutExtension()
      val chapter =
        ChapterRow(
          id = index,
          title = title,
          start = currentStart,
          end = currentStart + track.duration,
        )
      currentStart = chapter.end
      chapter
    }

  return if (chapters.any { it.title.isBlank() }) {
    Result.failure(IllegalArgumentException("Invalid track filename"))
  } else {
    Result.success(chapters)
  }
}

private fun String.fileNameWithoutExtension(): String {
  val filename = substringAfterLast('/').substringAfterLast('\\')
  val extensionStart = filename.lastIndexOf('.')
  return if (extensionStart > 0) filename.substring(0, extensionStart) else filename
}
