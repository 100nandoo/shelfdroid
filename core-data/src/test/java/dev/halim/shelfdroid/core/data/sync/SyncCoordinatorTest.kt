package dev.halim.shelfdroid.core.data.sync

import dev.halim.shelfdroid.core.UserPrefs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncCoordinatorTest {

  @Test
  fun prepareSync_skipsCurrentUserAfterLogin_andSyncsItWhenUserRequested() = runTest {
    val events = mutableListOf<SyncEvent>()
    val coordinator =
      SyncCoordinator(
        refreshCurrentUser = { events += SyncEvent.UserRequested },
        readUserPrefs = { null },
        refreshListeningStats = { _, _ -> },
        refreshAdminData = {},
      )

    coordinator.prepareSync(SyncEvent.AfterLogin)
    coordinator.prepareSync(SyncEvent.UserRequested)

    assertEquals(listOf(SyncEvent.UserRequested), events)
  }

  @Test
  fun syncBackgroundData_refreshesAdminDataAndAllListeningStatsForAdmin() = runTest {
    var listeningStatsRequest: Pair<Boolean, String?>? = null
    var adminRefresh: Boolean? = null
    val coordinator =
      SyncCoordinator(
        refreshCurrentUser = {},
        readUserPrefs = { UserPrefs(id = "admin", isAdmin = true) },
        refreshListeningStats = { isAdmin, userId ->
          listeningStatsRequest = isAdmin to userId
        },
        refreshAdminData = { adminRefresh = it },
      )

    coordinator.syncBackgroundData()

    assertEquals(true to "admin", listeningStatsRequest)
    assertEquals(true, adminRefresh)
  }

  @Test
  fun syncBackgroundData_refreshesOnlyUserListeningStatsForNonAdmin() = runTest {
    var listeningStatsRequest: Pair<Boolean, String?>? = null
    var adminRefresh: Boolean? = null
    val coordinator =
      SyncCoordinator(
        refreshCurrentUser = {},
        readUserPrefs = { UserPrefs(id = "listener") },
        refreshListeningStats = { isAdmin, userId ->
          listeningStatsRequest = isAdmin to userId
        },
        refreshAdminData = { adminRefresh = it },
      )

    coordinator.syncBackgroundData()

    assertEquals(false to "listener", listeningStatsRequest)
    assertEquals(false, adminRefresh)
  }
}
