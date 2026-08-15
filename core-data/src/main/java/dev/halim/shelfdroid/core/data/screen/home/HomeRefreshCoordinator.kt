package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.data.listening.ListeningStatsRepository
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

enum class HomeRefreshIntent {
  AfterLogin,
  UserRequested,
}

class HomeRefreshCoordinator
internal constructor(
  private val refreshCurrentUser: suspend () -> Unit,
  private val readUserPrefs: suspend () -> UserPrefs?,
  private val refreshListeningStats: (Boolean, String?) -> Unit,
  private val refreshAdminData: (Boolean) -> Unit,
) {

  @Inject
  constructor(
    currentUserSynchronizer: CurrentUserSynchronizer,
    listeningStatsRepository: ListeningStatsRepository,
    adminDataRefresher: AdminDataRefresher,
    prefsRepository: PrefsRepository,
    @Named("io") ioScope: CoroutineScope,
  ) : this(
    refreshCurrentUser = { currentUserSynchronizer.synchronize() },
    readUserPrefs = { prefsRepository.userPrefs.firstOrNull() },
    refreshListeningStats = { isAdmin, userId ->
      ioScope.launch {
        if (isAdmin) {
          listeningStatsRepository.refreshListeningStats()
        } else {
          userId?.takeIf(String::isNotBlank)?.let {
            listeningStatsRepository.refreshListeningStats(it)
          }
        }
      }
    },
    refreshAdminData = adminDataRefresher::refreshIfAdmin,
  )

  suspend fun prepareRefresh(intent: HomeRefreshIntent) {
    if (intent == HomeRefreshIntent.UserRequested) {
      refreshCurrentUser()
    }
  }

  suspend fun refreshBackgroundData() {
    val userPrefs = readUserPrefs()
    val isAdmin = userPrefs?.isAdmin == true
    refreshListeningStats(isAdmin, userPrefs?.id)
    refreshAdminData(isAdmin)
  }
}
