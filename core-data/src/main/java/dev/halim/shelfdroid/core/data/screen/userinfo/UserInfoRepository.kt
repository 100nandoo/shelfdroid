package dev.halim.shelfdroid.core.data.screen.userinfo

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.listening.ListeningStatsRepository
import dev.halim.shelfdroid.core.data.users.UserRepository
import javax.inject.Inject

class UserInfoRepository
@Inject
constructor(
  private val listeningStatsRepository: ListeningStatsRepository,
  private val userRepository: UserRepository,
  private val mapper: UserInfoMapper,
) {

  suspend fun item(userId: String): UserInfoUiState {
    listeningStatsRepository.refreshListeningStats(userId)
    val entity =
      listeningStatsRepository.byUserId(userId)
        ?: return UserInfoUiState(state = GenericState.Failure("Unable to load listening stats."))
    val mediaProgress = userRepository.fetchUserWithProgress(userId)

    val uiState = mapper.toUiState(entity, mediaProgress)
    return uiState
  }
}
