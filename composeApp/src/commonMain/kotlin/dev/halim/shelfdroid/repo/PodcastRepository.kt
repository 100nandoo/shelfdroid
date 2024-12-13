package dev.halim.shelfdroid.repo

import dev.halim.shelfdroid.Holder
import dev.halim.shelfdroid.db.model.Episode
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.MediaProgress
import dev.halim.shelfdroid.store.ItemKey
import dev.halim.shelfdroid.store.StoreManager
import dev.halim.shelfdroid.store.asSingle
import dev.halim.shelfdroid.store.cached
import dev.halim.shelfdroid.store.freshOrCached
import dev.halim.shelfdroid.ui.screens.detail.EpisodeUiState
import dev.halim.shelfdroid.ui.screens.detail.PodcastState
import dev.halim.shelfdroid.ui.screens.detail.PodcastUiState

class PodcastRepository(private val storeManager: StoreManager, private val api: Api) {

    fun generateUrl(episode: Episode): String {
        val url = api.generateItemStreamUrl(episode.libraryItemId, episode.ino)
        return url
    }

    inline fun toUiState(episode: Episode, cover: String, author: String, progress: MediaProgress? = null): EpisodeUiState {
        return EpisodeUiState(
            id = episode.id,
            author = author,
            title = episode.title,
            cover = cover,
            url = generateUrl(episode),
            seekTime = progress?.currentTime?.toLong()?.times(1000) ?: (0 * 1000),
            libraryItemId = episode.libraryItemId,
            publishedAt = episode.publishedAt,
            progress = progress?.progress ?: 0f,
        )
    }

    suspend fun getPodcastModel(episodeId: String): PodcastUiState {
        val userId = Holder.userId
        val user = storeManager.userStore.freshOrCached(userId)
        val progresses = user.mediaProgress
        val itemEntity = storeManager.itemStore.cached(ItemKey.Single(episodeId)).asSingle().data

        val cover = itemEntity.cover ?: ""
        val author = itemEntity.author ?: ""
        val description = itemEntity.description ?: ""

        val episodes = mutableListOf<EpisodeUiState>()
        itemEntity.episodes.forEach { episode ->
            val progress = progresses.firstOrNull { it.episodeId == episode.id }
            if (progress == null) {
                episodes.add(toUiState(episode, cover, author))
            } else {
                episodes.add(toUiState(episode, cover, author, progress))
            }
        }

        return PodcastUiState(
            state = PodcastState.Success,
            author = author,
            title = itemEntity.title,
            cover = cover,
            description = description,
            episodes = episodes
        )
    }
}