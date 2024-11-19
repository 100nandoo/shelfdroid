package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.SyncSessionRequest
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class SessionEvent {
    data class Play(val bookUiState: BookUiState) : SessionEvent()
    data class Pause(val deltaInSecond: Long) : SessionEvent()
}

class SessionManager(
    private val api: Api,
    private val io: CoroutineScope
) {
    private var bookUiState: BookUiState = BookUiState()
    private var sessionId: String = ""
    private val sessionInitialized = CompletableDeferred<Unit>()

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.Play -> {
                bookUiState = event.bookUiState
                io.launch {
                    startSession(event.bookUiState.id)
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
        val start = (bookUiState.seekTime / 1000)
        val currentInSecond = start + deltaInSecond
        val timeListened = currentInSecond - start
        val request = SyncSessionRequest(currentInSecond, timeListened)
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