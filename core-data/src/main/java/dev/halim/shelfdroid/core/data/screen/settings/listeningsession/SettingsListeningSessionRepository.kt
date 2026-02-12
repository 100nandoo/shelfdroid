package dev.halim.shelfdroid.core.data.screen.settings.listeningsession

import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.response.UserRepo
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User
import dev.halim.shelfdroid.core.database.UserEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SettingsListeningSessionRepository
@Inject
constructor(private val userRepo: UserRepo, private val prefsRepository: PrefsRepository) {

  fun users(): List<User> {
    return listOf(User.ALL_USER) + userRepo.all().map { map(it) }
  }

  suspend fun updateItemsPerPage(itemsPerPage: Int) {
    prefsRepository.updateListeningSessionPrefs(
      prefsRepository.listeningSessionPrefs.first().copy(itemsPerPage = itemsPerPage)
    )
  }

  private fun map(user: UserEntity): User {
    return User(user.id, user.username)
  }
}
