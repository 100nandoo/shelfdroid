package dev.halim.shelfdroid.core.data.podcast

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.Helper
import javax.inject.Inject

class PodcastRepository
@Inject
constructor(private val api: ApiService, private val helper: Helper) {

  suspend fun item(id: String): PodcastUiState {
    val result = api.item(id)
    result.onSuccess { response ->
      val media = response.media
      if (media is Podcast) {
        val cover = helper.generateItemCoverUrl(response.id)
        val author = media.metadata.author ?: ""
        val description = media.metadata.description ?: ""
        return PodcastUiState(
          state = GenericState.Success,
          author = author,
          title = media.metadata.title ?: "",
          cover = cover,
          description = description,
          episodes =
            media.episodes.map {
              Episode(
                it.title,
                it.publishedAt?.let { helper.toReadableDate(it) } ?: "",
                progress = 0.15f,
              )
            },
        )
      }

      return PodcastUiState(state = GenericState.Failure("Failed to parse podcast media"))
    }
    return PodcastUiState(state = GenericState.Failure("Failed to fetch podcast"))
  }
}
