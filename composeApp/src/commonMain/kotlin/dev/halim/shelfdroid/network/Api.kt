package dev.halim.shelfdroid.network

import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.datastore.DataStoreManager.DataStoreKeys.TOKEN
import dev.halim.shelfdroid.network.login.LoginRequest
import dev.halim.shelfdroid.network.login.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.firstOrNull

class Api(private val client: HttpClient, private val dataStoreManager: DataStoreManager) {
    companion object {
        var baseUrl = ""
        const val LOGIN_PATH = "/login"
        const val LOGOUT_PATH = "/logout"
        const val LIBRARIES_PATH = "/api/libraries"
        const val BATCH_LIBRARY_ITEMS = "/api/items/batch/get"
    }

    suspend fun <T> handleApiCall(
        successStateUpdater: suspend (T?) -> Unit,
        errorStateUpdater: suspend (String) -> Unit,
        apiCall: suspend () -> T
    ) {
        runCatching { apiCall() }
            .onSuccess { response -> successStateUpdater(response) }
            .onFailure { throwable ->
                println(throwable)
                val errorMessage = when (throwable) {
                    is UnauthorizedException -> "Invalid username or password."
                    else -> "Unknown error occurred"
                }
                errorStateUpdater(errorMessage)
            }
    }

    suspend fun login(loginRequest: LoginRequest): LoginResponse {
        return makeRequest("$baseUrl$LOGIN_PATH", HttpMethod.Post, loginRequest)
    }

    suspend fun logout(): LogoutResponse {
        return makeRequest("$baseUrl$LOGOUT_PATH", HttpMethod.Post)
    }

    suspend fun libraries(): LibrariesResponse {
        return makeRequest("$baseUrl$LIBRARIES_PATH", HttpMethod.Get)
    }

    suspend fun libraryItems(libraryId: String): LibraryItemsResponse {
        return makeRequest("$baseUrl/api/libraries/$libraryId/items", HttpMethod.Get)
    }

    suspend fun batchLibraryItems(batchLibraryItemsRequest: BatchLibraryItemsRequest): BatchLibraryItemsResponse {
        return makeRequest(
            "$baseUrl$BATCH_LIBRARY_ITEMS",
            HttpMethod.Post,
            batchLibraryItemsRequest
        )
    }


    private suspend inline fun <reified T> makeRequest(
        url: String,
        method: HttpMethod,
        body: Any? = null
    ): T {
        val token = dataStoreManager.dataStore.data.firstOrNull()?.get(TOKEN)

        val response: HttpResponse = client.request(url) {
            contentType(ContentType.Application.Json)
            if (token != null && url != "$baseUrl$LOGIN_PATH" && url != "$baseUrl$LOGOUT_PATH") {
                header("Authorization", "Bearer $token")
            }
            this.method = method
            when (method) {
                HttpMethod.Post -> {
                    if (body != null) setBody(body)
                }
                HttpMethod.Get -> {}
            }
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