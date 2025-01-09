package dev.halim.shelfdroid.ui

import dev.halim.shelfdroid.network.libraryitem.BookChapter
import kotlinx.serialization.Serializable

@Serializable
data class MediaItemBook(
    override val id: String = "",
    override val author: String = "",
    override val title: String = "",
    override val cover: String = "",
    override val url: String = "",
    override val seekTime: Long = 0,
    override val type: MediaItemType = MediaItemType.Book,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val currentChapter: BookChapter? = BookChapter(),
    val chapters: List<BookChapter> = emptyList()
) : ShelfdroidMediaItem() {
    constructor(item: MediaItemBook, newChapterIndex: Int) : this(
        item.id, item.author, item.title, item.cover, item.url, 0,
        MediaItemType.Book,
        item.chapters[newChapterIndex].start.toLong() * 1000,
        item.chapters[newChapterIndex].end.toLong() * 1000,
        item.chapters[newChapterIndex], item.chapters
    )
}

@Serializable
data class MediaItemPodcast(
    override val id: String = "",
    override val author: String = "",
    override val title: String = "",
    override val cover: String = "",
    override val url: String = "",
    override val seekTime: Long = 0,
    override val type: MediaItemType = MediaItemType.Podcast,
    val libraryItemId: String = "",
) : ShelfdroidMediaItem()

enum class MediaItemType {
    Book, Podcast
}

abstract class ShelfdroidMediaItem {
    abstract val id: String
    abstract val author: String
    abstract val title: String
    abstract val cover: String
    abstract val url: String
    abstract val seekTime: Long
    abstract val type: MediaItemType
}
