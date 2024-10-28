package dev.halim.shelfdroid.network

import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.network.login.LoginRequest
import dev.halim.shelfdroid.network.login.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class Api(private val client: HttpClient) {
    companion object {
        var baseUrl = ""
        const val LOGIN_PATH = "/login"
        const val LOGOUT_PATH = "/logout"
    }

    suspend fun <T> handleApiCall(
        uiStateUpdater: suspend (T?) -> Unit,
        errorStateUpdater: suspend (String) -> Unit,
        apiCall: suspend () -> T
    ) {
        runCatching { apiCall() }
            .onSuccess { response -> uiStateUpdater(response) }
            .onFailure { throwable ->
                val errorMessage = when (throwable) {
                    is UnauthorizedException -> "Invalid username or password."
                    else -> "Unknown error occurred"
                }
                errorStateUpdater(errorMessage)
            }
    }

    suspend fun login(loginRequest: LoginRequest): LoginResponse {
        return makePostRequest("$baseUrl$LOGIN_PATH", loginRequest)
    }

    suspend fun logout(): LogoutResponse {
        return makePostRequest("$baseUrl$LOGOUT_PATH")
    }

    private suspend inline fun <reified T> makePostRequest(
        url: String,
        body: Any? = null
    ): T {
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            if (body != null) setBody(body)
        }
        return handleResponse(response)
    }

    private suspend inline fun <reified T> handleResponse(response: HttpResponse): T {
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Unauthorized -> {
                val errorMessage = response.bodyAsText()
                throw UnauthorizedException(errorMessage)
            }

            else -> {
                val errorMessage = response.bodyAsText()
                throw Exception("Error ${response.status}: $errorMessage")
            }
        }
    }
}