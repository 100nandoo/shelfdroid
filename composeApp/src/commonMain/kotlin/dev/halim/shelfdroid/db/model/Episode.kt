package dev.halim.shelfdroid.db.model

import kotlinx.serialization.Serializable

@Serializable
data class Episode(
    val id: String = "",
    val libraryItemId: String = "",
    val ino: String = "",
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val publishedAt: Long = 0,
    val seekTime: Long = 0,
    val progress: Float = 0f
)