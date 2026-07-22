package dev.halim.core.network.request.emailmanagement

import dev.halim.core.network.response.emailmanagement.EreaderDevice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateEreaderDevicesRequest(
  @SerialName("ereaderDevices") val ereaderDevices: List<EreaderDevice> = emptyList()
)
