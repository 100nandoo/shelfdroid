package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.SyncSessionRequest
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class SessionEvent {
    data class Play(val shelfdroidMediaItem: ShelfdroidMediaItemImpl) : SessionEvent()
    data class Pause(val currentPosition: Long) : SessionEvent()
}

class SessionManager(
    private val api: Api,
    private val io: CoroutineScope
) {
    private var startTime: Long = -1
    private var sessionId: String = ""
    private val sessionInitialized = CompletableDeferred<Unit>()

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.Play -> {
                val newStartTime = (event.shelfdroidMediaItem.seekTime + event.shelfdroidMediaItem.startTime) / 1000
                if (newStartTime > startTime && startTime == -1L){
                    startTime = newStartTime
                }
                io.launch {
                    startSession(event.shelfdroidMediaItem.id)
                    sessionInitialized.complete(Unit)
                }
            }

            is SessionEvent.Pause -> {
                io.launch {
                    sessionInitialized.await()
                    syncSession(event.currentPosition)
                }
            }
        }
    }

    private suspend fun syncSession(currentPosition: Long) {
        val timeListened = currentPosition - startTime
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