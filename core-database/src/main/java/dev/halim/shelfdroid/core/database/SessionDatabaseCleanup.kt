package dev.halim.shelfdroid.core.database

class SessionDatabaseCleanup
internal constructor(
  private val inTransaction: (() -> Unit) -> Unit,
  private val deleteSessionScopedTables: () -> Unit,
) {
  constructor(
    database: MyDatabase
  ) : this(
    inTransaction = { deleteTables ->
      database.libraryEntityQueries.transaction { deleteTables() }
    },
    deleteSessionScopedTables = {
      database.localSessionEntityQueries.deleteAll()
      database.progressEntityQueries.deleteAll()
      database.bookmarkEntityQueries.deleteAll()
      database.listeningStatEntityQueries.deleteAll()
      database.podcastEpisodeEntityQueries.deleteAll()
      database.bookEntityQueries.deleteAll()
      database.podcastEntityQueries.deleteAll()
      database.libraryItemEntityQueries.deleteAll()
      database.libraryEntityQueries.deleteAll()
      database.userEntityQueries.deleteAll()
    },
  )

  fun clearSessionScopedData() {
    inTransaction(deleteSessionScopedTables)
  }
}
