package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.SyncSessionRequest
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class SessionEvent {
    data class ChangeItem(val shelfdroidMediaItem: ShelfdroidMediaItemImpl) : SessionEvent()
    data class Play(val shelfdroidMediaItem: ShelfdroidMediaItemImpl) : SessionEvent()
    data class Pause(val currentPosition: Long, val timeListened: Long) : SessionEvent()
}

class SessionManager(
    private val api: Api,
    private val io: CoroutineScope
) {
    private var sessionId: String = ""
    private val sessionInitialized = CompletableDeferred<Unit>()

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.ChangeItem -> Unit
            is SessionEvent.Play -> {
                io.launch {
                    sessionInitialized.await()
                    startSession(event.shelfdroidMediaItem.id)
                }
            }
            is SessionEvent.Pause -> {
                io.launch {
                    syncSession(event.currentPosition, event.timeListened)
                }
            }

        }
    }

    private suspend fun syncSession(currentPosition: Long, timeListened: Long) {
        sessionInitialized.complete(Unit)
        val request = SyncSessionRequest(currentPosition, timeListened)
        val response = api.syncSession(sessionId, request)
        response.isSuccess
    }

    private suspend fun startSession(itemId: String) {
        val result = api.playBook(itemId)
        result.onSuccess { response ->
            sessionId = response.id
        }
    }
}