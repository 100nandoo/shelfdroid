package dev.halim.shelfdroid.core.data.screen.usersettings

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.response.UserRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class UserSettingsRepository
@Inject
constructor(
  private val userRepo: UserRepo,
  private val mapper: UserSettingsMapper,
  private val prefsRepository: PrefsRepository,
) {

  fun item(): Flow<UserSettingsUiState> {
    val result =
      combine(userRepo.flowAll(), prefsRepository.userPrefs) { entityList, userPrefs ->
        val users = entityList.map { entity -> mapper.user(entity) }
        val isLoginUserRoot = userPrefs.type.isRoot()
        UserSettingsUiState(
          state = GenericState.Success,
          isLoginUserRoot = isLoginUserRoot,
          users = users,
        )
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
