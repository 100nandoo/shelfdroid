package dev.halim.shelfdroid.mapper

import dev.halim.shelfdroid.db.model.Episode
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.MediaProgress
import dev.halim.shelfdroid.ui.screens.podcast.EpisodeUiState

class Mapper(private val api: Api) {
    private fun generateUrl(episode: Episode): String {
        val url = api.generateItemStreamUrl(episode.libraryItemId, episode.ino)
        return url
    }

    fun toUiState(episode: Episode, cover: String, author: String, progress: MediaProgress? = null):
            EpisodeUiState {
        return EpisodeUiState(
            id = episode.id,
            author = author,
            title = episode.title,
            cover = cover,
            url = generateUrl(episode),
            seekTime = progress?.currentTime?.toLong()?.times(1000) ?: (0 * 1000),
            libraryItemId = episode.libraryItemId,
            description = episode.description,
            publishedAt = episode.publishedAt,
            progress = progress?.progress ?: 0f,
        )
    }

}