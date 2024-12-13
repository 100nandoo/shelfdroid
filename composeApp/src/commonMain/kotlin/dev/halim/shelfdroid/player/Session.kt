package dev.halim.shelfdroid.player

import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.SyncSessionRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class SessionEvent {
    data class PlayBook(val itemId: String) : SessionEvent()
    data class PlayPodcast(val itemId: String, val episodeId: String) : SessionEvent()
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
            is SessionEvent.PlayBook -> {
                io.launch {
                    if (sessionId.isNotEmpty()){
                        sessionInitialized.await()
                    }
                    startBookSession(event.itemId)
                }
            }
            is SessionEvent.PlayPodcast -> {
                io.launch {
                    if (sessionId.isNotEmpty()){
                        sessionInitialized.await()
                    }
                    startPodcastSession(event.itemId, event.episodeId)
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

    private suspend fun startBookSession(itemId: String) {
        val result = api.playBook(itemId)
        result.onSuccess { response ->
            sessionId = response.id
        }
    }

    private suspend fun startPodcastSession(itemId: String, episodeId: String) {
        val result = api.playPodcast(itemId, episodeId)
        result.onSuccess { response ->
            sessionId = response.id
        }
    }

}