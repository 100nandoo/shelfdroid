package dev.halim.shelfdroid.helper

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

fun ContentResolver.getFilename(uri: Uri): String {
  val fallback = uri.lastPathSegment ?: "file"

  return query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
    if (!cursor.moveToFirst()) return@use null

    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    if (columnIndex == -1) return@use null

    cursor.getString(columnIndex)
  } ?: fallback
}
