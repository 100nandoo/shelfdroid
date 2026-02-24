package dev.halim.shelfdroid.core.data.response

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CreateUserRequest
import dev.halim.core.network.request.UpdateUserRequest
import dev.halim.core.network.response.CreateUserResponse
import dev.halim.core.network.response.DeleteUserResponse
import dev.halim.core.network.response.Permissions
import dev.halim.core.network.response.Session
import dev.halim.core.network.response.UpdateUserResponse
import dev.halim.core.network.response.User
import dev.halim.core.network.response.UsersResponse
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.database.UserEntity
import dev.halim.shelfdroid.core.extensions.toLong
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class UserRepo
@Inject
constructor(
  db: MyDatabase,
  private val api: ApiService,
  private val coroutineScope: CoroutineScope,
) {

  private val queries = db.userEntityQueries

  fun flowAll(): Flow<List<UserEntity>> = queries.all().asFlow().mapToList(Dispatchers.IO)

  fun all(): List<UserEntity> = queries.all().executeAsList()

  fun byId(id: String) = queries.byId(id).executeAsOneOrNull()

  suspend fun create(request: CreateUserRequest): Result<CreateUserResponse> {
    val result = api.createUser(request)
    val response = result.getOrNull()
    if (response != null) {
      val entity = toEntity(response.user)
      queries.insert(entity)
    }
    return result
  }

  suspend fun remote(include: String? = null): Result<UsersResponse> {
    return fetch(include)
  }

  suspend fun update(id: String, request: UpdateUserRequest): Result<UpdateUserResponse> {
    val result = api.updateUser(id, request)
    val response = result.getOrNull()
    if (response != null) update(response.user)
    return result
  }

  suspend fun delete(id: String): Result<DeleteUserResponse> {
    val result = api.deleteUser(id)
    val response = result.getOrNull()
    if (response != null) cleanup(id)
    return result
  }

  private suspend fun fetch(include: String? = null): Result<UsersResponse> {
    val result = api.users(include)
    val response = result.getOrNull()
    if (response != null) save(response)
    return result
  }

  private fun save(usersResponse: UsersResponse) {
    val entities = usersResponse.users.map { toEntity(it) }
    coroutineScope.launch {
      cleanup(entities)
      entities.forEach { entity -> queries.insert(entity) }
    }
  }

  private fun update(user: User) {
    val permissions = Json.encodeToString(Permissions.serializer(), user.permissions)
    coroutineScope.launch {
      queries.transaction {
        val old = queries.byId(user.id).executeAsOne()
        val new =
          old.copy(
            username = user.username,
            email = user.email ?: "",
            type = UserType.toUserType(user.type.name),
            isActive = user.isActive.toLong(),
            permissions = permissions,
            itemTagsAccessible = user.itemTagsSelected,
            librariesAccessible = user.librariesAccessible,
          )
        queries.insert(new)
      }
    }
  }

  private fun cleanup(entities: List<UserEntity>) {
    queries.transaction {
      val ids = queries.allIds().executeAsList()
      val newIds = entities.map { it.id }
      val toDelete = ids.filter { !newIds.contains(it) }
      toDelete.forEach { queries.deleteById(it) }
    }
  }

  private fun cleanup(id: String) {
    queries.deleteById(id)
  }

  private fun toEntity(user: User): UserEntity {
    val latestSession = user.latestSession?.let { Json.encodeToString(Session.serializer(), it) }
    val permissions = Json.encodeToString(Permissions.serializer(), user.permissions)

    val result =
      UserEntity(
        id = user.id,
        username = user.username,
        email = user.email ?: "",
        type = UserType.toUserType(user.type.name),
        isActive = user.isActive.toLong(),
        lastSeen = user.lastSeen,
        latestSession = latestSession,
        permissions = permissions,
        librariesAccessible = user.librariesAccessible,
        itemTagsAccessible = user.itemTagsSelected,
      )

    return result
  }
}
