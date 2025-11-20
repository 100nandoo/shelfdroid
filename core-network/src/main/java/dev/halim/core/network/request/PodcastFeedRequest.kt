package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable data class PodcastFeedRequest(val rssFeed: String)
