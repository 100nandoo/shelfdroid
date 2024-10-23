package dev.halim.shelfdroid.network

import dev.halim.shelfdroid.network.login.LoginRequest
import dev.halim.shelfdroid.network.login.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class Api(private val client: HttpClient) {
    companion object {
        var baseUrl = ""
    }
    suspend fun login(loginRequest: LoginRequest): LoginResponse {
        val response = client.post("$baseUrl/login"){
            contentType(ContentType.Application.Json)
            setBody(loginRequest)
        }.body<LoginResponse>()
        return response
    }
}