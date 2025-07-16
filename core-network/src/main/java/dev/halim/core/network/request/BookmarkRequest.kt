package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable data class BookmarkRequest(val time: Long, val title: String)
