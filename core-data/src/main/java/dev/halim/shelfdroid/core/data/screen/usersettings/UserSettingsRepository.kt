package dev.halim.shelfdroid.core.data.screen.usersettings

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.UserRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserSettingsRepository
@Inject
constructor(private val userRepo: UserRepo, private val mapper: UserSettingsMapper) {

  fun item(): Flow<UserSettingsUiState> {
    val result =
      userRepo.flowAll().map { list ->
        val users = list.map { entity -> mapper.user(entity) }
        UserSettingsUiState(users = users, state = GenericState.Success)
      }
    return result
  }

  suspend fun remote() {
    userRepo.remote("latestSession")
  }

  suspend fun deleteUser(userId: String): UserSettingsApiState {
    userRepo.delete(userId).getOrElse {
      return UserSettingsApiState.DeleteFailure(it.message)
    }
    return UserSettingsApiState.DeleteSuccess
  }
}
