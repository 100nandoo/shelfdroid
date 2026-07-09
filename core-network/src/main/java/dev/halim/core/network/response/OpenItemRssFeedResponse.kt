package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class OpenItemRssFeedResponse(@SerialName("feed") val feed: RssFeed)
