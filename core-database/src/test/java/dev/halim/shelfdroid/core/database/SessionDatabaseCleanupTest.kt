package dev.halim.shelfdroid.core.database

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDatabaseCleanupTest {

  @Test
  fun clearSessionScopedData_deletesTablesInsideOneTransaction() {
    val events = mutableListOf<String>()
    val cleanup =
      SessionDatabaseCleanup(
        inTransaction = { deleteTables ->
          events += "transaction started"
          deleteTables()
          events += "transaction finished"
        },
        deleteSessionScopedTables = { events += "tables deleted" },
      )

    cleanup.clearSessionScopedData()

    assertEquals(
      listOf("transaction started", "tables deleted", "transaction finished"),
      events,
    )
  }
}
