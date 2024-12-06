package dev.halim.shelfdroid.db

import app.cash.sqldelight.ColumnAdapter
import dev.halim.shelfdroid.network.AudioBookmark
import dev.halim.shelfdroid.network.MediaProgress
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DatabaseAdapter(private val json: Json) {
    val listOfStringsAdapter = object : ColumnAdapter<List<String>, String> {
        override fun decode(databaseValue: String) =
            if (databaseValue.isEmpty()) {
                listOf()
            } else {
                databaseValue.split(",")
            }

        override fun encode(value: List<String>) = value.joinToString(separator = ",")
    }

    val listOfBookChaptersAdapter = object : ColumnAdapter<List<BookChapter>, String> {
        override fun decode(databaseValue: String): List<BookChapter> =
            if (databaseValue.isBlank()) {
                listOf()
            } else {
                json.decodeFromString<List<BookChapter>>(databaseValue)
            }

        override fun encode(value: List<BookChapter>): String = json.encodeToString(value)
    }

    val listOfMediaProgressAdapter = object : ColumnAdapter<List<MediaProgress>, String> {
        override fun decode(databaseValue: String): List<MediaProgress> =
            if (databaseValue.isBlank()) {
                listOf()
            } else {
                json.decodeFromString<List<MediaProgress>>(databaseValue)
            }

        override fun encode(value: List<MediaProgress>): String = json.encodeToString(value)
    }

    val listOfAudioBookmarkAdapter = object : ColumnAdapter<List<AudioBookmark>, String> {
        override fun decode(databaseValue: String): List<AudioBookmark> =
            if (databaseValue.isBlank()) {
                listOf()
            } else {
                json.decodeFromString<List<AudioBookmark>>(databaseValue)
            }

        override fun encode(value: List<AudioBookmark>): String = json.encodeToString(value)
    }

}