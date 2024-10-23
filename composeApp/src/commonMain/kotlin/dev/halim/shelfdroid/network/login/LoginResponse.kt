package dev.halim.shelfdroid.network.login


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("ereaderDevices")
    val ereaderDevices: List<String> = listOf(),
    @SerialName("serverSettings")
    val serverSettings: ServerSettings = ServerSettings(),
    @SerialName("Source")
    val source: String = "",
    @SerialName("user")
    val user: User = User(),
    @SerialName("userDefaultLibraryId")
    val userDefaultLibraryId: String = ""
)

@Serializable
data class LoginRequest(val username: String, val password: String)