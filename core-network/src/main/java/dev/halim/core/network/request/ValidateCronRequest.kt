package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class ValidateCronRequest(@SerialName("expression") val expression: String)
