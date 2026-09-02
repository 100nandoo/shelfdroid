package dev.halim.shelfdroid.core.data.task

import dev.halim.core.network.response.ServerTask as NetworkServerTask
import dev.halim.socketio.SocketEvent
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

/**
 * Application-scoped owner of Server task state. The repository has no polling loop: the only
 * snapshot requests are initial bootstrap, explicit refresh, and socket reconnection recovery.
 */
@Singleton
class TaskRepository
private constructor(
  private val api: TaskApi,
  private val socket: TaskSocket,
  private val catalogSynchronizer: TaskCatalogSynchronizer,
  @Named("io") private val scope: CoroutineScope,
  private val json: Json,
  private val clock: TaskClock,
  private val terminalRetentionMillis: Long,
  startImmediately: Boolean,
) : TaskRepositoryContract {

  private data class TaskReducerEffects(
    val task: Task,
    val shouldSynchronize: Boolean,
    val shouldNotify: Boolean,
    val shouldScheduleExpiry: Boolean,
  )

  @Inject
  constructor(
    api: TaskApi,
    socket: TaskSocket,
    catalogSynchronizer: TaskCatalogSynchronizer,
    @Named("io") scope: CoroutineScope,
    json: Json,
    clock: TaskClock,
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
      api: TaskApi,
      socket: TaskSocket,
      catalogSynchronizer: TaskCatalogSynchronizer,
      scope: CoroutineScope,
      json: Json,
      clock: TaskClock,
      terminalRetentionMillis: Long = TERMINAL_RETENTION_MILLIS,
    ): TaskRepository =
      TaskRepository(
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
  private val tasks = LinkedHashMap<String, Task>()
  private val acceptedTasks = LinkedHashMap<String, Task>()
  private val pendingTaskLibraries = mutableSetOf<String>()
  private val completedPendingTaskLibraries = mutableSetOf<String>()
  private val expiryJobs = mutableMapOf<String, Job>()
  private val notifiedTaskIds = mutableSetOf<String>()
  private var snapshotRequestGeneration = 0L
  private val _state = MutableStateFlow(TaskRepositoryState())
  override val state: StateFlow<TaskRepositoryState> = _state.asStateFlow()
  private val pendingNotifications = LinkedHashMap<String, TaskNotification>()
  private val _notifications = MutableStateFlow<TaskNotification?>(null)
  override val notifications: StateFlow<TaskNotification?> = _notifications.asStateFlow()

  private val owner: AutoCloseable = socket.acquire()
  private val subscriptions: List<AutoCloseable>

  init {
    subscriptions =
      listOf(
        socket.subscribe(SocketEvent.Task.Started, ::handleTaskEvent),
        socket.subscribe(SocketEvent.Task.Finished, ::handleTaskEvent),
        socket.subscribe(SocketEvent.Connect) {
          setConnection(TaskConnectionState.CONNECTED)
          scope.launch { refresh() }
        },
        socket.subscribe(SocketEvent.Disconnect) {
          setConnection(TaskConnectionState.DISCONNECTED)
        },
        socket.subscribe(SocketEvent.ConnectError) {
          setConnection(TaskConnectionState.DISCONNECTED)
        },
      )
    if (socket.isConnected()) setConnection(TaskConnectionState.CONNECTED)
    // Bootstrap once. This is intentionally not repeated on a timer.
    if (startImmediately) scope.launch { refresh() }
  }

  override suspend fun refresh(): Result<Unit> = refreshInternal()

  /**
   * The task endpoint is authoritative for active work, but an accepted operation remains visible
   * until its real task is observed. This matters when the HTTP response races task creation or a
   * reconnect/refresh snapshot races the socket event.
   */
  private suspend fun refreshInternal(): Result<Unit> {
    val requestGeneration = synchronized(lock) { ++snapshotRequestGeneration }
    _state.value = _state.value.copy(snapshotKnown = false)
    return api
      .tasks()
      .fold(
        onSuccess = { response ->
          val effects: MutableList<TaskReducerEffects>
          synchronized(lock) {
            // An explicit refresh and reconnect recovery can overlap. Do not let an older HTTP
            // response replace state that a newer snapshot (or its socket events) already settled.
            if (requestGeneration != snapshotRequestGeneration) return@fold Result.success(Unit)
            val snapshotActiveIds =
              response.tasks.asSequence().filter { !it.isFinished }.map { it.id }.toSet()
            // The server task endpoint is authoritative for active work. Preserve terminal rows
            // during their retention window and accepted HTTP-start placeholders, but drop active
            // rows that disappeared while the socket was disconnected.
            tasks.entries.removeIf { (id, task) ->
              val stale =
                task.status == TaskStatus.ACTIVE &&
                  id !in snapshotActiveIds &&
                  acceptedTasks.values.none { accepted ->
                    accepted.id == id
                  }
              if (stale) expiryJobs.remove(id)?.cancel()
              stale
            }
            effects = response.tasks.map { reduceTaskLocked(it.toDomainTask()) }.toMutableList()
            mergeAcceptedTasksLocked(response.tasks.map { it.id }.toSet())
            _state.value =
              _state.value.copy(
                snapshotKnown = true,
                tasks =
                  tasks.values.sortedWith(
                    compareByDescending<Task> { it.startedAt ?: 0L }.thenBy { it.id }
                  ),
              )
          }
          dispatchEffects(effects)
          Result.success(Unit)
        },
        onFailure = { error ->
          synchronized(lock) {
            if (requestGeneration == snapshotRequestGeneration) {
              _state.value = _state.value.copy(snapshotKnown = false)
            }
          }
          Result.failure(error)
        },
      )
  }

  override suspend fun startLibraryScan(libraryId: String): Result<Unit> {
    return startLibraryOperation(
      libraryId = libraryId,
      action = TaskAction.LibraryScan,
      placeholderPrefix = "accepted-scan",
      request = { api.scanLibrary(libraryId) },
    )
  }

  override suspend fun startLibraryMatch(libraryId: String): Result<Unit> {
    return startLibraryOperation(
      libraryId = libraryId,
      action = TaskAction.BookMatching,
      placeholderPrefix = "accepted-match",
      request = { api.matchLibrary(libraryId) },
    )
  }

  private suspend fun startLibraryOperation(
    libraryId: String,
    action: TaskAction,
    placeholderPrefix: String,
    request: suspend () -> Result<Unit>,
  ): Result<Unit> {
    val requestStartedAt = clock.now()
    synchronized(lock) {
      val hasActiveTask =
        tasks.values.any { it.libraryId == libraryId && it.status == TaskStatus.ACTIVE }
      if (libraryId in pendingTaskLibraries || hasActiveTask) {
        return Result.failure(IllegalStateException("Library already has an active task"))
      }
      pendingTaskLibraries += libraryId
    }
    return request()
      .fold(
        onSuccess = {
          // Audiobookshelf deliberately sends 200 before creating the Server task. Keep an accepted
          // active placeholder so controls stay gated until task_started or task_finished.
          synchronized(lock) {
            pendingTaskLibraries.remove(libraryId)
            val observedCompletion = libraryId in completedPendingTaskLibraries
            completedPendingTaskLibraries.remove(libraryId)
            val hasKnownActiveTask =
              tasks.values.any {
                it.libraryId == libraryId && it.status == TaskStatus.ACTIVE
              }
            val hasTaskCompletedDuringRequest =
              tasks.values.any {
                it.libraryId == libraryId &&
                  it.status != TaskStatus.ACTIVE &&
                  maxOf(it.startedAt ?: Long.MIN_VALUE, it.finishedAt ?: Long.MIN_VALUE) >=
                    requestStartedAt
              }
            if (!hasKnownActiveTask && !observedCompletion && !hasTaskCompletedDuringRequest) {
              val placeholderId = "$placeholderPrefix-$libraryId-${System.nanoTime()}"
              acceptedTasks[libraryId] =
                Task(
                  id = placeholderId,
                  action = action,
                  libraryId = libraryId,
                  status = TaskStatus.ACTIVE,
                  startedAt = clock.now(),
                )
            }
            publishLocked()
          }
          // Recover a task that may have been created before the HTTP response reached us. A
          // missing or failed snapshot must not turn an accepted operation into a reported start
          // error. The reducer retains its accepted placeholder through this and later snapshots.
          refreshInternal()
          Result.success(Unit)
        },
        onFailure = {
          synchronized(lock) {
            pendingTaskLibraries.remove(libraryId)
            completedPendingTaskLibraries.remove(libraryId)
          }
          Result.failure(it)
        },
      )
  }

  override suspend fun retrySynchronization(taskId: String): Result<Unit> {
    val task =
      synchronized(lock) { tasks[taskId] }
        ?: return Result.failure(IllegalArgumentException("Unknown task"))
    if (task.status != TaskStatus.COMPLETED || task.syncState != TaskSyncState.FAILED) {
      return Result.failure(IllegalStateException("Task is not waiting for synchronization retry"))
    }
    updateTask(task.copy(syncState = TaskSyncState.SYNCHRONIZING, syncError = null))
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
          it.copy(syncState = TaskSyncState.SUCCEEDED, syncError = null)
        }
        Result.success(Unit)
      } else {
        val error = result.exceptionOrNull() ?: IllegalStateException()
        updateTaskById(taskId) {
          it.copy(syncState = TaskSyncState.FAILED, syncError = error.toTaskError())
        }
        Result.failure(error)
      }
    } catch (error: Throwable) {
      if (error is CancellationException) throw error
      updateTaskById(taskId) {
        it.copy(syncState = TaskSyncState.FAILED, syncError = error.toTaskError())
      }
      Result.failure(error)
    }
  }

  private fun handleTaskEvent(args: Array<Any>) {
    val payload = args.firstOrNull() ?: return
    val raw = payload.toString()
    val task = runCatching { json.decodeFromString<NetworkServerTask>(raw) }.getOrNull() ?: return
    val domainTask = task.toDomainTask()
    var effects: TaskReducerEffects
    synchronized(lock) {
      val libraryId = domainTask.libraryId
      val accepted = libraryId?.let(acceptedTasks::get)
      if (libraryId != null && accepted != null && accepted.action == domainTask.action) {
        acceptedTasks.remove(libraryId)
        tasks.remove(accepted.id)
      }
      if (
        domainTask.status != TaskStatus.ACTIVE &&
          libraryId != null &&
          libraryId in pendingTaskLibraries
      ) {
        completedPendingTaskLibraries += libraryId
      }
      effects = reduceTaskLocked(domainTask)
      if (libraryId != null && effects.task.status == TaskStatus.ACTIVE) {
        val pending = libraryId in pendingTaskLibraries
        if (pending || accepted != null) acceptedTasks[libraryId] = effects.task
      }
    }
    dispatchEffects(effects)
  }

  /** Applies every task source through the same transition and effect calculation. */
  private fun reduceTaskLocked(incoming: Task): TaskReducerEffects {
    val previous = tasks[incoming.id]
    val next =
      when {
        // Server tasks are monotonic: a stale active snapshot/event must not undo completion.
        previous != null &&
          previous.status != TaskStatus.ACTIVE &&
          incoming.status == TaskStatus.ACTIVE -> previous
        previous?.status == TaskStatus.COMPLETED && incoming.status == TaskStatus.COMPLETED ->
          incoming.copy(syncState = previous.syncState, syncError = previous.syncError)
        else -> incoming
      }
    val shouldSynchronize =
      next.status == TaskStatus.COMPLETED &&
        (previous == null ||
          previous.status != TaskStatus.COMPLETED ||
          previous.syncState == TaskSyncState.NOT_STARTED)
    val published =
      if (shouldSynchronize) {
        next.copy(syncState = TaskSyncState.SYNCHRONIZING, syncError = null)
      } else {
        next
      }
    tasks[published.id] = published
    if (published.status == TaskStatus.ACTIVE) {
      expiryJobs.remove(published.id)?.cancel()
    }
    publishLocked()
    return TaskReducerEffects(
      task = published,
      shouldSynchronize = shouldSynchronize,
      shouldNotify = published.status != TaskStatus.ACTIVE && previous?.status != published.status,
      shouldScheduleExpiry = published.status != TaskStatus.ACTIVE,
    )
  }

  private fun dispatchEffects(effects: List<TaskReducerEffects>) {
    effects.forEach { effect ->
      if (effect.shouldNotify) enqueueNotification(effect.task)
      if (effect.shouldSynchronize) scope.launch { synchronizeTask(effect.task.id) }
      if (effect.shouldScheduleExpiry) scheduleExpiry(effect.task.id)
    }
  }

  private fun dispatchEffects(effect: TaskReducerEffects) = dispatchEffects(listOf(effect))

  private fun scheduleExpiry(taskId: String) {
    synchronized(lock) {
      if (expiryJobs[taskId]?.isActive == true) return
    }
    expiryJobs[taskId]?.cancel()
    expiryJobs[taskId] = scope.launch {
      delay(terminalRetentionMillis(taskId))
      synchronized(lock) {
        tasks.remove(taskId)
        expiryJobs.remove(taskId)
        publishLocked()
      }
    }
  }

  private fun enqueueNotification(task: Task) {
    if (task.status == TaskStatus.ACTIVE) return
    val shouldNotify = synchronized(lock) { notifiedTaskIds.add(task.id) }
    if (!shouldNotify) return
    synchronized(lock) {
      pendingNotifications[task.id] =
        TaskNotification(
          taskId = task.id,
          status = task.status,
          action = task.action,
        )
      _notifications.value = pendingNotifications.values.firstOrNull()
    }
  }

  private fun terminalRetentionMillis(taskId: String): Long {
    val finishedAt = synchronized(lock) { tasks[taskId]?.finishedAt }
    return if (finishedAt == null) {
      terminalRetentionMillis
    } else {
      (terminalRetentionMillis - (clock.now() - finishedAt)).coerceAtLeast(0L)
    }
  }

  private fun mergeAcceptedTasksLocked(snapshotTaskIds: Set<String>) {
    acceptedTasks.values.toList().forEach { accepted ->
      val task =
        tasks.values.firstOrNull {
          it.id != accepted.id &&
            it.id in snapshotTaskIds &&
            it.libraryId == accepted.libraryId &&
            it.action == accepted.action &&
            (it.status == TaskStatus.ACTIVE ||
              maxOf(it.startedAt ?: Long.MIN_VALUE, it.finishedAt ?: Long.MIN_VALUE) >=
                (accepted.startedAt ?: Long.MIN_VALUE))
        }
      if (task == null) {
        // Keep accepted placeholders (or accepted real socket tasks) through every snapshot until
        // the server reports the corresponding task. This preserves action gating during the
        // HTTP/socket creation race without polling.
        tasks[accepted.id] = accepted
      } else {
        acceptedTasks.remove(accepted.libraryId)
        if (task.id != accepted.id) tasks.remove(accepted.id)
      }
    }
  }

  private fun updateTask(task: Task) {
    synchronized(lock) {
      tasks[task.id] = task
      publishLocked()
    }
  }

  private fun updateTaskById(taskId: String, transform: (Task) -> Task) {
    synchronized(lock) {
      tasks[taskId]?.let { tasks[taskId] = transform(it) }
      publishLocked()
    }
  }

  private fun setConnection(connectionState: TaskConnectionState) {
    _state.value =
      _state.value.copy(
        connectionState = connectionState,
        snapshotKnown =
          if (connectionState == TaskConnectionState.DISCONNECTED) false
          else _state.value.snapshotKnown,
      )
  }

  private fun publishLocked() {
    _state.value =
      _state.value.copy(
        tasks =
          tasks.values.sortedWith(compareByDescending<Task> { it.startedAt ?: 0L }.thenBy { it.id })
      )
  }

  /**
   * Exception messages are implementation details; only server-provided task text is displayable.
   */
  private fun Throwable.toTaskError(): TaskError = TaskError.Generic
}
