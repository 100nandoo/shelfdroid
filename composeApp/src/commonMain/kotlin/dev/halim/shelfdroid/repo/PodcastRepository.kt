package dev.halim.shelfdroid.repo

import dev.halim.shelfdroid.Holder
import dev.halim.shelfdroid.mapper.Mapper
import dev.halim.shelfdroid.store.ItemKey
import dev.halim.shelfdroid.store.StoreManager
import dev.halim.shelfdroid.store.asSingle
import dev.halim.shelfdroid.store.cached
import dev.halim.shelfdroid.store.freshOrCached
import dev.halim.shelfdroid.ui.screens.podcast.EpisodeUiState
import dev.halim.shelfdroid.ui.screens.podcast.PodcastState
import dev.halim.shelfdroid.ui.screens.podcast.PodcastUiState

class PodcastRepository(private val storeManager: StoreManager, private val mapper: Mapper) {

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
                episodes.add(mapper.toUiState(episode, cover, author))
            } else {
                episodes.add(mapper.toUiState(episode, cover, author, progress))
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