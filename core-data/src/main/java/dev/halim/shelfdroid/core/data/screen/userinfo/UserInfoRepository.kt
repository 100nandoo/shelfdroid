package dev.halim.shelfdroid.core.data.screen.userinfo

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.ListeningStatRepo
import javax.inject.Inject

class UserInfoRepository
@Inject
constructor(private val repo: ListeningStatRepo, private val mapper: UserInfoMapper) {

  fun item(userId: String): UserInfoUiState {
    val entity = repo.byUserId(userId) ?: return UserInfoUiState(state = GenericState.Failure())

    val uiState = mapper.toUiState(entity)
    return uiState
  }
}
