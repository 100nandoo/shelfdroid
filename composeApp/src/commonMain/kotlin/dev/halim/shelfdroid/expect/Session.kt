package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.SyncSessionRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class SessionEvent {
    data class Play(val itemId: String, val startTime: Long, val duration: Long) : SessionEvent()
    data class PlaySameItem(val startTime: Long) : SessionEvent()
    data class Pause(val current: Long) : SessionEvent()
}

class SessionManager(
    private val api: Api,
    private val io: CoroutineScope
) {
    private var startTime: Long = 0L
    private var duration: Long = 0L
    private var sessionId: String = ""
    private val sessionInitialized = CompletableDeferred<Unit>()

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.Play -> {
                startTime = event.startTime
                duration = event.duration
                io.launch {
                    startSession(event.itemId)
                    sessionInitialized.complete(Unit)
                }
            }
            is SessionEvent.PlaySameItem-> {
                startTime = event.startTime
            }
            is SessionEvent.Pause -> {
                io.launch {
                    sessionInitialized.await()
                    syncSession(event.current)
                }
            }
        }
    }

    private suspend fun syncSession(current: Long) {
        val timeListened = current - startTime
        val request = SyncSessionRequest(current, timeListened, duration)
        api.syncSession(sessionId, request).collect { response ->
            response.isSuccess
        }
    }

    private suspend fun startSession(itemId: String) {
        api.playBook(itemId).collect { result ->
            result.onSuccess { response ->
                sessionId = response.id
            }
        }
    }
}