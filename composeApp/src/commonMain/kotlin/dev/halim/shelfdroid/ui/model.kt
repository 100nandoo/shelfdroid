package dev.halim.shelfdroid.ui

import dev.halim.shelfdroid.network.libraryitem.BookChapter
import kotlinx.serialization.Serializable

@Serializable
data class ShelfdroidMediaItemImpl(
    val id: String = "", val author: String = "", val title: String = "", val cover: String = "",
    val url: String = "", val seekTime: Long = 0, val startTime: Long = 0, val endTime: Long = 0,
    val currentChapter: BookChapter, val chapters: List<BookChapter>
) {
    constructor(item: ShelfdroidMediaItemImpl, newChapterIndex: Int) : this(
        item.id, item.author, item.title, item.cover, item.url, 0,
        item.chapters[newChapterIndex].start.toLong() * 1000,
        item.chapters[newChapterIndex].end.toLong() * 1000,
        item.chapters[newChapterIndex], item.chapters
    )
}

abstract class ShelfdroidMediaItem {
    abstract val id: String
    abstract val author: String
    abstract val title: String
    abstract val cover: String
    abstract val url: String
    abstract val seekTime: Long
    abstract val startTime: Long
    abstract val endTime: Long
    abstract val currentChapter: BookChapter
    abstract val chapters: List<BookChapter>
    abstract fun toImpl(): ShelfdroidMediaItemImpl
}
