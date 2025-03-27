package dev.halim.shelfdroid.core.data.podcast

sealed class PodcastState {
    data object Loading : PodcastState()
    data object Success : PodcastState()
    data class Failure(val errorMessage: String?) : PodcastState()
}

data class PodcastUiState(
    val state: PodcastState = PodcastState.Loading,
    val author: String = "",
    val title: String = "",
    val cover: String = "",
    val description: String = "",
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val title: String = "",
    val publishedAt: String = "",
    val progress: Float = 0f,
)