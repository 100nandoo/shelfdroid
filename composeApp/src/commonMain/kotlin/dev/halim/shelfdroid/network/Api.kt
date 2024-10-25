package dev.halim.shelfdroid.network

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
    }
    suspend fun login(loginRequest: LoginRequest): LoginResponse {
        val response: HttpResponse = client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(loginRequest)
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                response.body<LoginResponse>()
            }
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