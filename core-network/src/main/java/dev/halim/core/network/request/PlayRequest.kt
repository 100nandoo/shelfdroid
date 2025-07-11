package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable
data class PlayRequest(
  val deviceInfo: DeviceInfo,
  val mediaPlayer: String,
  val forceTranscode: Boolean,
  val forceDirectPlay: Boolean,
)

@Serializable
data class DeviceInfo(
  val deviceId: String,
  val manufacturer: String,
  val model: String,
  val osVersion: String,
  val sdkVersion: Int,
  val clientName: String,
  val clientVersion: String,
)
