package dev.halim.shelfdroid.repo

import dev.halim.shelfdroid.network.libraryitem.BookChapter

class RepoHelper {
    fun getCurrentChapter(currentTime: Float, chapters: List<BookChapter>): BookChapter? {
        return (chapters.find { currentTime >= it.start && currentTime <= it.end } ?: chapters.first())
    }
}