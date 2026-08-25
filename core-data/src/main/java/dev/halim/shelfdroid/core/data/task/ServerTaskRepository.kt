package dev.halim.shelfdroid.core.data.task

import dev.halim.core.network.response.ServerTask as NetworkServerTask
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Connection state used by the application-scoped Server task repository. */
enum class ServerTaskConnectionState {
  UNKNOWN,
  CONNECTED,
  DISCONNECTED,
}

enum class ServerTaskStatus {
  ACTIVE,
  COMPLETED,
  FAILED,
  CANCELLED,
}

enum class ServerTaskSyncState {
  NOT_STARTED,
  SYNCHRONIZING,
  SUCCEEDED,
  FAILED,
}

sealed interface ServerTaskError {
  data class SafeMessage(val message: String) : ServerTaskError

  data object Generic : ServerTaskError
}

data class ServerTaskResult(
  val added: Int? = null,
  val updated: Int? = null,
  val missing: Int? = null,
  val elapsedMillis: Long? = null,
)

/** A stable app model for every server task, independent of the operation that created it. */
data class ServerTask(
  val id: String,
  val action: String,
  val libraryId: String?,
  val title: String? = null,
  val status: ServerTaskStatus,
  val startedAt: Long? = null,
  val finishedAt: Long? = null,
  val result: ServerTaskResult? = null,
  val error: ServerTaskError? = null,
  val syncState: ServerTaskSyncState = ServerTaskSyncState.NOT_STARTED,
  val syncError: ServerTaskError? = null,
)

data class ServerTaskRepositoryState(
  val connectionState: ServerTaskConnectionState = ServerTaskConnectionState.UNKNOWN,
  val snapshotKnown: Boolean = false,
  val tasks: List<ServerTask> = emptyList(),
)

data class ServerTaskNotification(
  val taskId: String,
  val status: ServerTaskStatus,
  val action: String? = null,
)

interface ServerTaskRepositoryContract {
  val state: StateFlow<ServerTaskRepositoryState>
  /** The oldest unacknowledged terminal notification, retained across screen recreation. */
  val notifications: StateFlow<ServerTaskNotification?>

  suspend fun refresh(): Result<Unit>

  /** The HTTP response means accepted/started; task completion is never inferred here. */
  suspend fun startLibraryScan(libraryId: String): Result<Unit>

  /** The match-all HTTP response means accepted/started; completion is socket/task state. */
  suspend fun startLibraryMatch(libraryId: String): Result<Unit>

  suspend fun retrySynchronization(taskId: String): Result<Unit>

  fun acknowledgeNotification(taskId: String)
}

/**
 * Application-scoped owner of Server task state. The repository has no polling loop: the only
 * snapshot requests are initial bootstrap, explicit refresh, and socket reconnection recovery.
 */
@Singleton
class ServerTaskRepository private constructor(
  private val api: ServerTaskApi,
  private val socket: ServerTaskSocket,
  private val catalogSynchronizer: ServerTaskCatalogSynchronizer,
  @Named("io") private val scope: CoroutineScope,
  private val json: Json,
  private val clock: ServerTaskClock,
  private val terminalRetentionMillis: Long,
  startImmediately: Boolean,
) : ServerTaskRepositoryContract {

  @Inject
  constructor(
    api: ServerTaskApi,
    socket: ServerTaskSocket,
    catalogSynchronizer: ServerTaskCatalogSynchronizer,
    @Named("io") scope: CoroutineScope,
    json: Json,
    clock: ServerTaskClock,
  ) : this(
    api,
    socket,
    catalogSynchronizer,
    scope,
    json,
    clock,
    TERMINAL_RETENTION_MILLIS,
    startImmediately = true,
  )

  internal companion object {
    const val TERMINAL_RETENTION_MILLIS = 60_000L

    fun forTest(
      api: ServerTaskApi,
      socket: ServerTaskSocket,
      catalogSynchronizer: ServerTaskCatalogSynchronizer,
      scope: CoroutineScope,
      json: Json,
      clock: ServerTaskClock,
      terminalRetentionMillis: Long = TERMINAL_RETENTION_MILLIS,
    ): ServerTaskRepository =
      ServerTaskRepository(
        api,
        socket,
        catalogSynchronizer,
        scope,
        json,
        clock,
        terminalRetentionMillis,
        startImmediately = false,
      )
  }

  private val lock = Any()
  private val tasks = LinkedHashMap<String, ServerTask>()
  private val acceptedTasks = LinkedHashMap<String, ServerTask>()
  private val pendingTaskLibraries = mutableSetOf<String>()
  private val expiryJobs = mutableMapOf<String, Job>()
  private val notifiedTaskIds = mutableSetOf<String>()
  private val _state = MutableStateFlow(ServerTaskRepositoryState())
  override val state: StateFlow<ServerTaskRepositoryState> = _state.asStateFlow()
  private val pendingNotifications = LinkedHashMap<String, ServerTaskNotification>()
  private val _notifications = MutableStateFlow<ServerTaskNotification?>(null)
  override val notifications: StateFlow<ServerTaskNotification?> = _notifications.asStateFlow()

  private val owner: AutoCloseable = socket.acquire()
  private val subscriptions: List<AutoCloseable>

  init {
    subscriptions =
      listOf(
        socket.subscribe("task_started") { args -> handleTaskEvent(args, finished = false) },
        socket.subscribe("task_finished") { args -> handleTaskEvent(args, finished = true) },
        socket.subscribe("connect") {
          setConnection(ServerTaskConnectionState.CONNECTED)
          scope.launch { refresh() }
        },
        socket.subscribe("disconnect") {
          setConnection(ServerTaskConnectionState.DISCONNECTED)
        },
        socket.subscribe("connect_error") {
          setConnection(ServerTaskConnectionState.DISCONNECTED)
        },
      )
    if (socket.isConnected()) setConnection(ServerTaskConnectionState.CONNECTED)
    // Bootstrap once. This is intentionally not repeated on a timer.
    if (startImmediately) scope.launch { refresh() }
  }

  override suspend fun refresh(): Result<Unit> = refreshInternal()

  /**
   * A library operation endpoint can return before the task manager creates its task. Preserve an
   * accepted placeholder only for the one recovery snapshot immediately following that response.
   */
  private suspend fun refreshInternal(
    preserveAcceptedTaskLibraries: Set<String> = emptySet(),
  ): Result<Unit> {
    _state.value = _state.value.copy(snapshotKnown = false)
    return api.tasks().fold(
      onSuccess = { response ->
        val terminalTaskIds = mutableListOf<String>()
        val synchronizationTaskIds = mutableListOf<String>()
        val notificationTasks = mutableListOf<ServerTask>()
        synchronized(lock) {
          val snapshotActiveIds =
            response.tasks
              .asSequence()
              .filter { !it.isFinished }
              .map { it.id }
              .toSet()
          // The server task endpoint is authoritative for active work. Preserve terminal rows
          // during their retention window and accepted HTTP-start placeholders, but drop active
          // rows that disappeared while the socket was disconnected.
          tasks.entries.removeIf { (id, task) ->
            val stale =
              task.status == ServerTaskStatus.ACTIVE &&
                id !in snapshotActiveIds &&
                acceptedTasks.values.none { accepted ->
                  accepted.id == id && accepted.libraryId in preserveAcceptedTaskLibraries
                }
            if (stale) expiryJobs.remove(id)?.cancel()
            stale
          }
          response.tasks.forEach { task ->
            val mapped =
              task.toDomainTask().let { next ->
                val previous = tasks[task.id]
                if (
                  previous?.status == ServerTaskStatus.COMPLETED &&
                    next.status == ServerTaskStatus.COMPLETED
                ) {
                  next.copy(syncState = previous.syncState, syncError = previous.syncError)
                } else {
                  next
                }
              }
            val previous = tasks[task.id]
            tasks[task.id] = mapped
            if (mapped.status == ServerTaskStatus.ACTIVE) {
              expiryJobs.remove(mapped.id)?.cancel()
            } else {
              terminalTaskIds += mapped.id
              if (previous?.status != mapped.status) {
                notificationTasks += mapped
              }
              if (
                mapped.status == ServerTaskStatus.COMPLETED &&
                  (previous == null ||
                    previous.status != ServerTaskStatus.COMPLETED ||
                    previous.syncState == ServerTaskSyncState.NOT_STARTED)
              ) {
                synchronizationTaskIds += mapped.id
              }
            }
          }
          mergeAcceptedTasksLocked(
            response.tasks.map { it.id }.toSet(),
            preserveAcceptedTaskLibraries,
          )
          _state.value =
            _state.value.copy(
              snapshotKnown = true,
              tasks =
                tasks.values.sortedWith(
                  compareByDescending<ServerTask> { it.startedAt ?: 0L }.thenBy { it.id }
                ),
            )
        }
        terminalTaskIds.forEach(::scheduleExpiry)
        notificationTasks.forEach(::enqueueNotification)
        synchronizationTaskIds.distinct().forEach { taskId ->
          updateTaskById(taskId) {
            it.copy(syncState = ServerTaskSyncState.SYNCHRONIZING, syncError = null)
          }
          scope.launch { synchronizeTask(taskId) }
        }
        Result.success(Unit)
      },
      onFailure = { error ->
        _state.value = _state.value.copy(snapshotKnown = false)
        Result.failure(error)
      },
    )
  }

  override suspend fun startLibraryScan(libraryId: String): Result<Unit> {
    return startLibraryOperation(
      libraryId = libraryId,
      action = "library-scan",
      placeholderPrefix = "accepted-scan",
      request = { api.scanLibrary(libraryId) },
    )
  }

  override suspend fun startLibraryMatch(libraryId: String): Result<Unit> {
    return startLibraryOperation(
      libraryId = libraryId,
      action = "library-match-all",
      placeholderPrefix = "accepted-match",
      request = { api.matchLibrary(libraryId) },
    )
  }

  private suspend fun startLibraryOperation(
    libraryId: String,
    action: String,
    placeholderPrefix: String,
    request: suspend () -> Result<Unit>,
  ): Result<Unit> {
    val requestStartedAt = clock.now()
    synchronized(lock) {
      val hasActiveTask =
        tasks.values.any { it.libraryId == libraryId && it.status == ServerTaskStatus.ACTIVE }
      if (libraryId in pendingTaskLibraries || hasActiveTask) {
        return Result.failure(IllegalStateException("Library already has an active task"))
      }
      pendingTaskLibraries += libraryId
    }
    return request().fold(
      onSuccess = {
        // Audiobookshelf deliberately sends 200 before creating the Server task. Keep an accepted
        // active placeholder so controls stay gated until task_started or task_finished.
        synchronized(lock) {
          pendingTaskLibraries.remove(libraryId)
          val hasKnownActiveTask =
            tasks.values.any {
              it.libraryId == libraryId &&
                it.status == ServerTaskStatus.ACTIVE
            }
          val hasTaskCompletedDuringRequest =
            tasks.values.any {
              it.libraryId == libraryId &&
                it.status != ServerTaskStatus.ACTIVE &&
                maxOf(it.startedAt ?: Long.MIN_VALUE, it.finishedAt ?: Long.MIN_VALUE) >=
                  requestStartedAt
            }
          if (!hasKnownActiveTask && !hasTaskCompletedDuringRequest) {
            val placeholderId = "$placeholderPrefix-$libraryId-${System.nanoTime()}"
            acceptedTasks[libraryId] =
              ServerTask(
                id = placeholderId,
                action = action,
                libraryId = libraryId,
                status = ServerTaskStatus.ACTIVE,
                startedAt = clock.now(),
              )
          }
          publishLocked()
        }
        // Recover a task that may have been created before the HTTP response reached us. A
        // missing or failed snapshot must not turn an accepted operation into a reported start
        // error.
        refreshInternal(preserveAcceptedTaskLibraries = setOf(libraryId))
        Result.success(Unit)
      },
      onFailure = {
        synchronized(lock) { pendingTaskLibraries.remove(libraryId) }
        Result.failure(it)
      },
    )
  }

  override suspend fun retrySynchronization(taskId: String): Result<Unit> {
    val task =
      synchronized(lock) { tasks[taskId] }
        ?: return Result.failure(IllegalArgumentException("Unknown task"))
    if (task.status != ServerTaskStatus.COMPLETED || task.syncState != ServerTaskSyncState.FAILED) {
      return Result.failure(IllegalStateException("Task is not waiting for synchronization retry"))
    }
    updateTask(task.copy(syncState = ServerTaskSyncState.SYNCHRONIZING, syncError = null))
    return synchronizeTask(task.id)
  }

  override fun acknowledgeNotification(taskId: String) {
    synchronized(lock) {
      pendingNotifications.remove(taskId)
      _notifications.value = pendingNotifications.values.firstOrNull()
    }
  }

  private suspend fun synchronizeTask(taskId: String): Result<Unit> {
    return try {
      val result = catalogSynchronizer.synchronize()
      if (result.isSuccess) {
        updateTaskById(taskId) {
          it.copy(syncState = ServerTaskSyncState.SUCCEEDED, syncError = null)
        }
        Result.success(Unit)
      } else {
        val error =
          result.exceptionOrNull() ?: IllegalStateException()
        updateTaskById(taskId) {
          it.copy(syncState = ServerTaskSyncState.FAILED, syncError = error.toServerTaskError())
        }
        Result.failure(error)
      }
    } catch (error: Throwable) {
      if (error is CancellationException) throw error
      updateTaskById(taskId) {
        it.copy(syncState = ServerTaskSyncState.FAILED, syncError = error.toServerTaskError())
      }
      Result.failure(error)
    }
  }

  private fun handleTaskEvent(args: Array<Any>, finished: Boolean) {
    val payload = args.firstOrNull() ?: return
    val raw = payload.toString()
    val task = runCatching { json.decodeFromString<NetworkServerTask>(raw) }.getOrNull() ?: return
    var domainTask = task.toDomainTask()
    var shouldSynchronize = false
    synchronized(lock) {
      val previous = tasks[domainTask.id]
      if (
        previous?.status == ServerTaskStatus.COMPLETED &&
          domainTask.status == ServerTaskStatus.COMPLETED
      ) {
        domainTask =
          domainTask.copy(syncState = previous.syncState, syncError = previous.syncError)
      }
      val pendingTask =
        domainTask.libraryId != null && domainTask.libraryId in pendingTaskLibraries
      if (domainTask.libraryId != null) {
        acceptedTasks.remove(domainTask.libraryId)?.let { accepted -> tasks.remove(accepted.id) }
        if (pendingTask && domainTask.status == ServerTaskStatus.ACTIVE) {
          acceptedTasks[domainTask.libraryId] = domainTask
        }
      }
      if (domainTask.status == ServerTaskStatus.ACTIVE) {
        expiryJobs.remove(domainTask.id)?.cancel()
      }
      shouldSynchronize =
        finished &&
          domainTask.status == ServerTaskStatus.COMPLETED &&
          (previous == null ||
            previous.status != ServerTaskStatus.COMPLETED ||
            previous.syncState == ServerTaskSyncState.NOT_STARTED)
      tasks[domainTask.id] = domainTask
      publishLocked()
    }
    if (finished && domainTask.status != ServerTaskStatus.ACTIVE) enqueueNotification(domainTask)
    if (shouldSynchronize) {
      updateTaskById(domainTask.id) {
        it.copy(syncState = ServerTaskSyncState.SYNCHRONIZING)
      }
      scope.launch { synchronizeTask(domainTask.id) }
    }
    if (domainTask.status != ServerTaskStatus.ACTIVE) scheduleExpiry(domainTask.id)
  }

  private fun scheduleExpiry(taskId: String) {
    synchronized(lock) {
      if (expiryJobs[taskId]?.isActive == true) return
    }
    expiryJobs[taskId]?.cancel()
    expiryJobs[taskId] =
      scope.launch {
        delay(terminalRetentionMillis(taskId))
        synchronized(lock) {
          tasks.remove(taskId)
          expiryJobs.remove(taskId)
          publishLocked()
        }
      }
  }

  private fun enqueueNotification(task: ServerTask) {
    if (task.status == ServerTaskStatus.ACTIVE) return
    val shouldNotify = synchronized(lock) { notifiedTaskIds.add(task.id) }
    if (!shouldNotify) return
    synchronized(lock) {
      pendingNotifications[task.id] =
        ServerTaskNotification(
          taskId = task.id,
          status = task.status,
          action = task.action.takeIf { it == "library-match-all" },
        )
      _notifications.value = pendingNotifications.values.firstOrNull()
    }
  }

  private fun terminalRetentionMillis(taskId: String): Long {
    val finishedAt = synchronized(lock) { tasks[taskId]?.finishedAt }
    return if (finishedAt == null) {
      terminalRetentionMillis
    } else {
      (terminalRetentionMillis - (clock.now() - finishedAt))
        .coerceAtLeast(0L)
    }
  }

  private fun mergeAcceptedTasksLocked(
    snapshotTaskIds: Set<String>,
    preserveAcceptedTaskLibraries: Set<String>,
  ) {
    acceptedTasks.values.toList().forEach { accepted ->
      val serverTask = tasks.values.firstOrNull {
        it.id != accepted.id &&
          it.libraryId == accepted.libraryId &&
          it.action == accepted.action &&
          (it.status == ServerTaskStatus.ACTIVE ||
            maxOf(it.startedAt ?: Long.MIN_VALUE, it.finishedAt ?: Long.MIN_VALUE) >=
              (accepted.startedAt ?: Long.MIN_VALUE))
      }
      if (serverTask == null) {
        if (accepted.libraryId in preserveAcceptedTaskLibraries) {
          tasks[accepted.id] = accepted
        } else {
          acceptedTasks.remove(accepted.libraryId)
          tasks.remove(accepted.id)
        }
      } else if (serverTask.id != accepted.id || serverTask.id in snapshotTaskIds) {
        acceptedTasks.remove(accepted.libraryId)
        if (serverTask.id != accepted.id) tasks.remove(accepted.id)
      } else if (accepted.libraryId !in preserveAcceptedTaskLibraries) {
        acceptedTasks.remove(accepted.libraryId)
        tasks.remove(accepted.id)
      }
    }
  }

  private fun updateTask(task: ServerTask) {
    synchronized(lock) {
      tasks[task.id] = task
      publishLocked()
    }
  }

  private fun updateTaskById(taskId: String, transform: (ServerTask) -> ServerTask) {
    synchronized(lock) {
      tasks[taskId]?.let { tasks[taskId] = transform(it) }
      publishLocked()
    }
  }

  private fun setConnection(connectionState: ServerTaskConnectionState) {
    _state.value =
      _state.value.copy(
        connectionState = connectionState,
        snapshotKnown =
          if (connectionState == ServerTaskConnectionState.DISCONNECTED) false
          else _state.value.snapshotKnown,
      )
  }

  private fun publishLocked() {
    _state.value =
      _state.value.copy(
        tasks =
          tasks.values.sortedWith(
            compareByDescending<ServerTask> { it.startedAt ?: 0L }.thenBy { it.id }
          ),
      )
  }

  /** Exception messages are implementation details; only server-provided task text is displayable. */
  private fun Throwable.toServerTaskError(): ServerTaskError = ServerTaskError.Generic

}

/** Pure mapping kept visible to module tests so socket and HTTP payloads share one reducer. */
internal fun NetworkServerTask.toDomainTask(): ServerTask {
  val taskData = data ?: emptyMap()
  val libraryId = taskData["libraryId"]?.jsonPrimitive?.content
  val scanResults = taskData["scanResults"]?.jsonObject
  val result =
    scanResults?.let {
      ServerTaskResult(
        added = it["added"]?.jsonPrimitive?.intOrNull,
        updated = it["updated"]?.jsonPrimitive?.intOrNull,
        missing = it["missing"]?.jsonPrimitive?.intOrNull,
        elapsedMillis = it["elapsed"]?.jsonPrimitive?.longOrNull,
      )
    }
  val status =
    when {
      !isFinished -> ServerTaskStatus.ACTIVE
      isFailed -> ServerTaskStatus.FAILED
      descriptionKey == "MessageTaskCanceledByUser" ||
        description?.contains("canceled", ignoreCase = true) == true -> ServerTaskStatus.CANCELLED
      else -> ServerTaskStatus.COMPLETED
    }
  return ServerTask(
    id = id,
    action = action,
    libraryId = libraryId,
    title = title,
    status = status,
    startedAt = startedAt,
    finishedAt = finishedAt,
    result = result,
    error = error.toServerTaskError(),
  )
}

private fun String?.toServerTaskError(): ServerTaskError? {
  if (isNullOrBlank()) return null
  return if (
    length <= 240 &&
      !contains("Exception", ignoreCase = true) &&
      !contains(" at ")
  ) {
    ServerTaskError.SafeMessage(this)
  } else {
    ServerTaskError.Generic
  }
}
