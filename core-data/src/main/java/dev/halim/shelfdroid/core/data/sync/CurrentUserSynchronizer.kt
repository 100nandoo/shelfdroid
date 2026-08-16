package dev.halim.shelfdroid.core.data.sync

import dev.halim.core.network.response.login.LoginResponse
import dev.halim.core.network.response.login.UserType as NetworkUserType
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.listening.BookmarkRepository
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.users.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull

class CurrentUserSynchronizer
@Inject
constructor(
  private val userRepository: UserRepository,
  private val progressRepository: ProgressRepository,
  private val bookmarkRepository: BookmarkRepository,
  private val prefsRepository: PrefsRepository,
) {

  suspend fun synchronize(): Result<Unit> {
    val response = userRepository.authorize()
    val loginResponse =
      response.getOrNull() ?: return Result.failure(requireNotNull(response.exceptionOrNull()))
    val user = loginResponse.user

    return try {
      progressRepository.replaceUserProgress(user)
      bookmarkRepository.replaceUserBookmarks(user)
      updateDataStore(loginResponse)
      Result.success(Unit)
    } catch (error: Throwable) {
      if (error is CancellationException) throw error
      Result.failure(error)
    }
  }

  private suspend fun updateDataStore(loginResponse: LoginResponse) {
    val user = loginResponse.user
    val old = prefsRepository.userPrefs.firstOrNull()?.copy()
    old?.let {
      val userPrefs =
        UserPrefs(
          id = user.id,
          username = user.username,
          type = UserType.toUserType(user.type.name),
          isAdmin = user.type == NetworkUserType.ADMIN || user.type == NetworkUserType.ROOT,
          download = user.permissions.download,
          upload = user.permissions.upload,
          delete = user.permissions.delete,
          update = user.permissions.update,
          accessToken = old.accessToken,
          refreshToken = old.refreshToken,
        )
      prefsRepository.updateUserPrefs(userPrefs)
    }

    prefsRepository.updateServerPrefs(loginResponse.serverSettings)
  }
}
