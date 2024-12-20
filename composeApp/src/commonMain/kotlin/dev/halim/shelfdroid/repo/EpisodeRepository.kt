package dev.halim.shelfdroid.repo

import dev.halim.shelfdroid.Holder
import dev.halim.shelfdroid.db.model.Episode
import dev.halim.shelfdroid.mapper.Mapper
import dev.halim.shelfdroid.store.ItemKey
import dev.halim.shelfdroid.store.StoreManager
import dev.halim.shelfdroid.store.asSingle
import dev.halim.shelfdroid.store.cached
import dev.halim.shelfdroid.store.freshOrCached
import dev.halim.shelfdroid.ui.generic.GenericState
import dev.halim.shelfdroid.ui.screens.episode.EpisodeScreenUiState

class EpisodeRepository(private val storeManager: StoreManager, private val mapper: Mapper) {
    suspend fun getEpisodeModel(podcastId: String, episodeId: String): EpisodeScreenUiState {
        val userId = Holder.userId
        val user = storeManager.userStore.freshOrCached(userId)
        val progresses = user.mediaProgress
        val itemEntity = storeManager.itemStore.cached(ItemKey.Single(podcastId)).asSingle().data

        val cover = itemEntity.cover ?: ""
        val author = itemEntity.author ?: ""


        val episode = itemEntity.episodes.firstOrNull { it.id == episodeId }
        val progress = progresses.firstOrNull { it.episodeId == episode?.id }

        return if (episode != null) {
            val episodeUiState = mapper.toUiState(episode, cover, author)
            EpisodeScreenUiState(state = GenericState.Success, episodeUiState = episodeUiState)
        } else {
            val episodeUiState = mapper.toUiState(Episode(), cover, author, progress)
            EpisodeScreenUiState(state = GenericState.Failure("Episode not found"), episodeUiState = episodeUiState)
        }
    }

}