package dev.halim.shelfdroid.core.data.screen.userinfo

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.ListeningStatRepo
import dev.halim.shelfdroid.core.data.response.UserRepo
import javax.inject.Inject

class UserInfoRepository
@Inject
constructor(
  private val repo: ListeningStatRepo,
  private val userRepo: UserRepo,
  private val mapper: UserInfoMapper,
) {

  suspend fun item(userId: String): UserInfoUiState {
    val entity = repo.byUserId(userId) ?: return UserInfoUiState(state = GenericState.Failure())
    val mediaProgress = userRepo.userWithProgress(userId)

    val uiState = mapper.toUiState(entity, mediaProgress)
    return uiState
  }
}
