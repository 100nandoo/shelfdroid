package dev.halim.shelfdroid.core.data.screen.listeningsession

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.response.UserRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ListeningSessionRepository
@Inject
constructor(
  private val api: ApiService,
  private val userRepo: UserRepo,
  private val mapper: ListeningSessionMapper,
  private val prefsRepository: PrefsRepository,
) {
  val listeningSessionPrefs = prefsRepository.listeningSessionPrefs

  suspend fun item(page: Int = 0): ListeningSessionUiState {
    val listeningSessionPrefs = prefsRepository.listeningSessionPrefs.first()
    val response =
      api
        .sessions(
          page = page,
          itemsPerPage = listeningSessionPrefs.itemsPerPage,
          user = listeningSessionPrefs.defaultUserId,
        )
        .getOrElse {
          return ListeningSessionUiState(state = GenericState.Failure(it.message))
        }

    val users = userRepo.all()

    return mapper.map(response, users, listeningSessionPrefs.defaultUserId)
  }

  suspend fun page(
    page: Int = 0,
    itemsPerPage: Int = 10,
    userId: String? = null,
    users: List<ListeningSessionUiState.User>,
  ): ListeningSessionUiState {
    val response =
      api.sessions(page = page, itemsPerPage = itemsPerPage, user = userId).getOrElse {
        return ListeningSessionUiState(state = GenericState.Failure(it.message))
      }
    return mapper.combine(response, users, userId)
  }

  suspend fun updateItemsPerPage(itemsPerPage: Int) {
    prefsRepository.updateListeningSessionPrefs(
      listeningSessionPrefs.first().copy(itemsPerPage = itemsPerPage)
    )
  }
}
