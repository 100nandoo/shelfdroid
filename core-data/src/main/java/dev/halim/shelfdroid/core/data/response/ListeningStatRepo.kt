package dev.halim.shelfdroid.core.data.response

import android.util.Log
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.halim.core.network.ApiService
import dev.halim.core.network.response.ListeningStatResponse
import dev.halim.shelfdroid.core.database.ListeningStatEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.supervisorScope

class ListeningStatRepo @Inject constructor(db: MyDatabase, private val api: ApiService) {

  private val queries = db.listeningStatEntityQueries
  private val userQueries = db.userEntityQueries

  fun flowAll(): Flow<List<ListeningStatEntity>> = queries.all().asFlow().mapToList(Dispatchers.IO)

  fun all(): List<ListeningStatEntity> = queries.all().executeAsList()

  fun byUserId(userId: String) = queries.byUserId(userId).executeAsOneOrNull()

  suspend fun remote() = supervisorScope {
    val userIds = userQueries.allIds().executeAsList()

    val results =
      userIds
        .map { userId ->
          async(Dispatchers.IO) {
            val result = api.listeningStats(userId)
            val response = result.getOrNull()
            if (result.isFailure) {
              Log.e("ListeningStatRepo", "remote: ${result.exceptionOrNull()}")
            }
            response?.let { userId to it }
          }
        }
        .awaitAll()
        .filterNotNull()

    if (results.isNotEmpty()) {
      save(results)
    }
  }

  fun save(data: List<Pair<String, ListeningStatResponse>>) {
    queries.transaction {
      data.forEach { (userId, response) ->
        val entity = toEntity(userId, response)
        queries.insert(entity)
      }
    }
  }

  private fun toEntity(userId: String, listeningStat: ListeningStatResponse): ListeningStatEntity {
    val result =
      ListeningStatEntity(
        userId = userId,
        totalTime = listeningStat.totalTime.toLong(),
        today = listeningStat.today.toLong(),
        days = listeningStat.days.mapValues { it.value.toInt() },
        dayOfWeek = listeningStat.dayOfWeek.mapValues { it.value.toInt() },
      )

    return result
  }
}
