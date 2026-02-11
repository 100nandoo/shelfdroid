package dev.halim.shelfdroid.core.data.screen.listeningsession

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class ListeningSessionRepository
@Inject
constructor(
  private val api: ApiService,
  private val mapper: ListeningSessionMapper,
  private val prefsRepository: PrefsRepository,
) {
  val listeningSessionPrefs = prefsRepository.listeningSessionPrefs

  suspend fun item(
    page: Int = 0,
    itemsPerPage: Int = 10,
    userId: String? = null,
  ): ListeningSessionUiState = coroutineScope {
    val sessionsDeferred = async {
      api.sessions(page = page, itemsPerPage = itemsPerPage, user = userId)
    }
    val usersDeferred = async { api.users() }

    val response =
      sessionsDeferred.await().getOrElse {
        return@coroutineScope ListeningSessionUiState(state = GenericState.Failure(it.message))
      }

    val users =
      usersDeferred
        .await()
        .getOrElse {
          return@coroutineScope ListeningSessionUiState(state = GenericState.Failure(it.message))
        }
        .users

    mapper.map(response, users)
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
