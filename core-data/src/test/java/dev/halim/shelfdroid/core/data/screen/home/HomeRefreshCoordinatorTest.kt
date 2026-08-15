package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.shelfdroid.core.UserPrefs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRefreshCoordinatorTest {

  @Test
  fun prepareRefresh_skipsCurrentUserAfterLogin_andRefreshesItWhenUserRequested() = runTest {
    val intents = mutableListOf<HomeRefreshIntent>()
    val coordinator =
      HomeRefreshCoordinator(
        refreshCurrentUser = { intents += HomeRefreshIntent.UserRequested },
        readUserPrefs = { null },
        refreshListeningStats = { _, _ -> },
        refreshAdminData = {},
      )

    coordinator.prepareRefresh(HomeRefreshIntent.AfterLogin)
    coordinator.prepareRefresh(HomeRefreshIntent.UserRequested)

    assertEquals(listOf(HomeRefreshIntent.UserRequested), intents)
  }

  @Test
  fun refreshBackgroundData_refreshesAdminDataAndAllListeningStatsForAdmin() = runTest {
    var listeningStatsRequest: Pair<Boolean, String?>? = null
    var adminRefresh: Boolean? = null
    val coordinator =
      HomeRefreshCoordinator(
        refreshCurrentUser = {},
        readUserPrefs = { UserPrefs(id = "admin", isAdmin = true) },
        refreshListeningStats = { isAdmin, userId ->
          listeningStatsRequest = isAdmin to userId
        },
        refreshAdminData = { adminRefresh = it },
      )

    coordinator.refreshBackgroundData()

    assertEquals(true to "admin", listeningStatsRequest)
    assertEquals(true, adminRefresh)
  }

  @Test
  fun refreshBackgroundData_refreshesOnlyUserListeningStatsForNonAdmin() = runTest {
    var listeningStatsRequest: Pair<Boolean, String?>? = null
    var adminRefresh: Boolean? = null
    val coordinator =
      HomeRefreshCoordinator(
        refreshCurrentUser = {},
        readUserPrefs = { UserPrefs(id = "listener") },
        refreshListeningStats = { isAdmin, userId ->
          listeningStatsRequest = isAdmin to userId
        },
        refreshAdminData = { adminRefresh = it },
      )

    coordinator.refreshBackgroundData()

    assertEquals(false to "listener", listeningStatsRequest)
    assertEquals(false, adminRefresh)
  }
}
