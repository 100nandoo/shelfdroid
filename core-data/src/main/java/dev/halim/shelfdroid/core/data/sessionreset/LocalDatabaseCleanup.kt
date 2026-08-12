package dev.halim.shelfdroid.core.data.sessionreset

import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Inject

class LocalDatabaseCleanup
internal constructor(
  private val inTransaction: (() -> Unit) -> Unit,
  private val deleteTables: () -> Unit,
) {
  @Inject
  constructor(
    database: MyDatabase
  ) : this(
    inTransaction = { deleteTables ->
      database.libraryEntityQueries.transaction { deleteTables() }
    },
    deleteTables = {
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

  fun clear() {
    inTransaction(deleteTables)
  }
}
