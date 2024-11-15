package dev.halim.shelfdroid.network

import dev.halim.shelfdroid.datastore.DataStoreManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class Api(private val client: HttpClient, private val dataStoreManager: DataStoreManager) {
    companion object {
        var baseUrl = ""
        const val LOGIN_PATH = "/login"
        const val LOGOUT_PATH = "/logout"
        const val LIBRARIES_PATH = "/api/libraries"
        const val BATCH_LIBRARY_ITEMS = "/api/items/batch/get"
        const val ME_PATH = "/api/me"
        const val SYNC_PROGRESS_PATH = "/api/session/%s/sync"
    }

    private fun mapErrorToMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnauthorizedException -> "Invalid username or password."
            else -> "Unknown error occurred"
        }
    }

     fun login(loginRequest: LoginRequest): Flow<Result<LoginResponse>> {
        return makeRequestFlow("$baseUrl$LOGIN_PATH", HttpMethod.Post, loginRequest)
    }

    fun logout(): Flow<Result<LogoutResponse>> {
        return makeRequestFlow("$baseUrl$LOGOUT_PATH", HttpMethod.Post)
    }

    fun libraries(): Flow<Result<LibrariesResponse>> {
        return makeRequestFlow("$baseUrl$LIBRARIES_PATH", HttpMethod.Get)
    }

    fun libraryItems(libraryId: String): Flow<Result<LibraryItemsResponse>> {
        return makeRequestFlow("$baseUrl/api/libraries/$libraryId/items", HttpMethod.Get)
    }

    fun me(): Flow<Result<User>> {
        return makeRequestFlow("$baseUrl$ME_PATH", HttpMethod.Get)
    }

    fun batchLibraryItems(libraryItemIds: List<String>): Flow<Result<BatchLibraryItemsResponse>> {
        return makeRequestFlow(
            "$baseUrl$BATCH_LIBRARY_ITEMS",
            HttpMethod.Post,
            BatchLibraryItemsRequest(libraryItemIds)
        )
    }

    fun syncProgress(id: String, syncProgressRequest: SyncProgressRequest): Flow<Result<Unit>>{
        val path = SYNC_PROGRESS_PATH.replace("%s", id)
        return makeRequestFlow("$baseUrl$path", HttpMethod.Post, syncProgressRequest)
    }

    fun generateItemCoverUrl(itemId: String): String {
        val token = dataStoreManager.tokenBlocking
        return "$baseUrl/api/items/$itemId/cover?token=$token"
    }

    fun generateItemStreamUrl(itemId: String, ino: String): String {
        val token = dataStoreManager.tokenBlocking
        return "$baseUrl/api/items/$itemId/file/$ino?token=$token"
    }


    private inline fun <reified T> makeRequestFlow(
        url: String,
        method: HttpMethod,
        body: Any? = null,
        queryParams: Map<String, String>? = null
    ): Flow<Result<T>> {
        return flow {
            val token = dataStoreManager.token.firstOrNull()

            try {
                val response: HttpResponse = client.request(url) {
                    contentType(ContentType.Application.Json)
                    if (token.isNullOrBlank().not()) {
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                    this.method = method

                    queryParams?.let { params ->
                        for ((key, value) in params) {
                            parameter(key, value)
                        }
                    }

                    when (method) {
                        HttpMethod.Post -> {
                            if (body != null) setBody(body)
                        }
                        HttpMethod.Get -> {}
                    }
                }

                val result = handleResponse<T>(response)
                emit(Result.success(result))

            } catch (e: Throwable) {
                println(e)
                val errorMessage = mapErrorToMessage(e)
                emit(Result.failure(Exception(errorMessage, e)))
            }
        }
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