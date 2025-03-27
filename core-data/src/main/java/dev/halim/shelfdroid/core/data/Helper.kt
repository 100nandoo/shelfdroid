package dev.halim.shelfdroid.core.data

import dev.halim.shelfdroid.core.datastore.DataStoreManager
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import javax.inject.Inject

class Helper @Inject constructor() {
    fun generateItemCoverUrl(itemId: String): String {
        return "https://${DataStoreManager.BASE_URL}/api/items/$itemId/cover"
    }

    fun generateItemStreamUrl(token: String, itemId: String, ino: String): String {
        return "https://${DataStoreManager.BASE_URL}/api/items/$itemId/file/$ino?token=$token"
    }

    fun toReadableDate(long: Long): String {
        val instant = Instant.fromEpochMilliseconds(long)
        val dateTime = instant.toLocalDateTime(TimeZone.UTC)
        return "${dateTime.dayOfMonth} ${dateTime.month.name.lowercase().capitalize(Locale.getDefault())} ${dateTime.year}"
    }

}