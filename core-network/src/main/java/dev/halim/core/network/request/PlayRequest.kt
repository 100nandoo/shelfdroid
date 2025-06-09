package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable
data class PlayRequest(
  val deviceInfo: DeviceInfo,
  //  val supportedMimeTypes: List<String> = listOf(),
  //  val mediaPlayer: String = "",
  val forceTranscode: Boolean,
  val forceDirectPlay: Boolean,
)

@Serializable data class DeviceInfo(val clientName: String, val deviceId: String)
