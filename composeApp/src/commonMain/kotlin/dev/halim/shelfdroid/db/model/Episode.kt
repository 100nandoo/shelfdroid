package dev.halim.shelfdroid.db.model

import kotlinx.serialization.Serializable

@Serializable
data class Episode(
    val id: String, val ino: String, val title: String, val subtitle: String, val description: String,
    val publishedAt: Long, val seekTime: Long, val progress: Float
)