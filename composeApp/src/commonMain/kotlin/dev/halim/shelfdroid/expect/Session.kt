package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.SyncSessionRequest
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class SessionEvent {
    data class Play(val shelfdroidMediaItem: ShelfdroidMediaItemImpl) : SessionEvent()
    data class Pause(val deltaInSecond: Long) : SessionEvent()
}

class SessionManager(
    private val api: Api,
    private val io: CoroutineScope
) {
    private var shelfdroidMediaItem: ShelfdroidMediaItemImpl = ShelfdroidMediaItemImpl()
    private var sessionId: String = ""
    private val sessionInitialized = CompletableDeferred<Unit>()

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.Play -> {
                shelfdroidMediaItem = event.shelfdroidMediaItem
                io.launch {
                    startSession(event.shelfdroidMediaItem.id)
                    sessionInitialized.complete(Unit)
                }
            }

            is SessionEvent.Pause -> {
                io.launch {
                    sessionInitialized.await()
                    syncSession(event.deltaInSecond)
                }
            }
        }
    }

    private suspend fun syncSession(deltaInSecond: Long) {
        val start = (shelfdroidMediaItem.seekTime / 1000)
        val currentInSecond = start + deltaInSecond
        val timeListened = currentInSecond - start
        val request = SyncSessionRequest(currentInSecond, timeListened)
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