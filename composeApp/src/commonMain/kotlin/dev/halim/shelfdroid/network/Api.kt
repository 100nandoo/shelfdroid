package dev.halim.shelfdroid.network

import dev.halim.shelfdroid.app_name
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.expect.deviceName
import dev.halim.shelfdroid.expect.manufacturer
import dev.halim.shelfdroid.expect.sdkVersion
import dev.halim.shelfdroid.expect.supportedMimeType
import dev.halim.shelfdroid.version
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class Api(private val client: HttpClient, private val dataStoreManager: DataStoreManager) {
    companion object {
        var baseUrl = ""
        const val LOGIN_PATH = "/login"
        const val LOGOUT_PATH = "/logout"
        const val LIBRARIES_PATH = "/api/libraries"
        const val BATCH_LIBRARY_ITEMS = "/api/items/batch/get"
        const val ME_PATH = "/api/me"
        const val SYNC_PROGRESS_PATH = "/api/session/%s/sync"
        const val PLAY_BOOK_PATH = "/api/items/%s/play"
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dataStoreManager.token.collect { token = it }
            dataStoreManager.deviceId.collect { deviceId = it }
        }
    }

    private var deviceId = dataStoreManager.deviceIdBlocking
    private var token = dataStoreManager.tokenBlocking
    private val deviceInfoRequest =
        DeviceInfoRequest(deviceId, app_name, version, manufacturer, deviceName, sdkVersion)

    private fun mapErrorToMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnauthorizedException -> "Invalid username or password."
            else -> "Unknown error occurred"
        }
    }

    suspend fun login(loginRequest: LoginRequest): Result<LoginResponse> {
        return makeRequest("$baseUrl$LOGIN_PATH", HttpMethod.Post, loginRequest)
    }

    suspend fun logout(): Result<LogoutResponse> {
        return makeRequest("$baseUrl$LOGOUT_PATH", HttpMethod.Post)
    }

    suspend fun libraries(): Result<LibrariesResponse> {
        return makeRequest("$baseUrl$LIBRARIES_PATH", HttpMethod.Get)
    }

    suspend fun libraryItems(libraryId: String): Result<LibraryItemsResponse> {
        return makeRequest("$baseUrl/api/libraries/$libraryId/items", HttpMethod.Get)
    }

    suspend fun me(): Result<User> {
        return makeRequest("$baseUrl$ME_PATH", HttpMethod.Get)
    }

    suspend fun batchLibraryItems(libraryItemIds: List<String>): Result<BatchLibraryItemsResponse> {
        return makeRequest(
            "$baseUrl$BATCH_LIBRARY_ITEMS",
            HttpMethod.Post,
            BatchLibraryItemsRequest(libraryItemIds)
        )
    }

    suspend fun playBook(itemId: String): Result<PlayBookResponse> {
        val path = PLAY_BOOK_PATH.replace("%s", itemId)
        return makeRequest(
            "$baseUrl$path",
            HttpMethod.Post,
            PlayBookRequest(deviceInfoRequest, false, false, supportedMimeType, app_name)
        )
    }

    suspend fun syncSession(id: String, syncSessionRequest: SyncSessionRequest): Result<Unit> {
        val path = SYNC_PROGRESS_PATH.replace("%s", id)
        return makeRequest("$baseUrl$path", HttpMethod.Post, syncSessionRequest)
    }

    fun generateItemCoverUrl(itemId: String): String {
        return "$baseUrl/api/items/$itemId/cover?token=$token"
    }

    fun generateItemStreamUrl(itemId: String, ino: String): String {
        return "$baseUrl/api/items/$itemId/file/$ino?token=$token"
    }

    private suspend inline fun <reified T> makeRequest(
        url: String,
        method: HttpMethod,
        body: Any? = null,
        queryParams: Map<String, String>? = null
    ): Result<T> {
        return try {
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
                    HttpMethod.Get -> {} // No body needed for GET
                }
            }

            val result = handleResponse<T>(response)
            Result.success(result)
        } catch (e: Throwable) {
            println(e)
            val errorMessage = mapErrorToMessage(e)
            Result.failure(Exception(errorMessage, e))
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