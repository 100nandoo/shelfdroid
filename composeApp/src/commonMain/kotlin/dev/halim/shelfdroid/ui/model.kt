package dev.halim.shelfdroid.ui

import kotlinx.serialization.Serializable

@Serializable
data class ShelfdroidMediaItemImpl(
    val id: String = "", val author: String = "", val title: String = "", val cover: String = "",
    val url: String = "", val seekTime: Long = 0, val startTime: Long = 0, val endTime: Long = 0
)

abstract class ShelfdroidMediaItem {
    abstract val id: String
    abstract val author: String
    abstract val title: String
    abstract val cover: String
    abstract val url: String
    abstract val seekTime: Long
    abstract val startTime: Long
    abstract val endTime: Long
    abstract fun toImpl(): ShelfdroidMediaItemImpl
}
