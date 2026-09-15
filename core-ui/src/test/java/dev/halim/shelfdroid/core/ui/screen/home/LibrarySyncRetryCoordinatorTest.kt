package dev.halim.shelfdroid.core.ui.screen.home

import dev.halim.shelfdroid.core.data.sync.SyncEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySyncRetryCoordinatorTest {

  @Test
  fun reconnectDuringOfflineSync_queuesOneRetry() {
    val coordinator = LibrarySyncRetryCoordinator()

    assertTrue(coordinator.tryStart(SyncEvent.AfterLogin))
    assertFalse(coordinator.tryStart(SyncEvent.NetworkAvailable))
    assertTrue(coordinator.finish(hadTransportFailure = true, hasInternet = true))
    assertTrue(coordinator.tryStart(SyncEvent.NetworkAvailable))
    assertFalse(coordinator.tryStart(SyncEvent.NetworkAvailable))
  }

  @Test
  fun reconnectDuringSuccessfulSync_doesNotRepeatSuccessfulSync() {
    val coordinator = LibrarySyncRetryCoordinator()

    assertTrue(coordinator.tryStart(SyncEvent.AfterLogin))
    assertFalse(coordinator.tryStart(SyncEvent.NetworkAvailable))
    assertFalse(coordinator.finish(hadTransportFailure = false, hasInternet = true))
    assertTrue(coordinator.tryStart(SyncEvent.NetworkAvailable))
  }
}
