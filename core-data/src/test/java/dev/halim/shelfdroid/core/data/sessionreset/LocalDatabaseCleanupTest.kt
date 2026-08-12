package dev.halim.shelfdroid.core.data.sessionreset

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDatabaseCleanupTest {

  @Test
  fun clear_deletesTablesInsideOneTransaction() {
    val events = mutableListOf<String>()
    val cleanup =
      LocalDatabaseCleanup(
        inTransaction = { deleteTables ->
          events += "transaction started"
          deleteTables()
          events += "transaction finished"
        },
        deleteTables = { events += "tables deleted" },
      )

    cleanup.clear()

    assertEquals(
      listOf("transaction started", "tables deleted", "transaction finished"),
      events,
    )
  }
}
