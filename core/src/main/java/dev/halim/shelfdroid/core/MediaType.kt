package dev.halim.shelfdroid.core

/** Media type used by application and domain models. */
enum class MediaType(val apiValue: String?) {
  BOOK("book"),
  PODCAST("podcast"),
  UNKNOWN(null);

  companion object {
    fun fromApiValue(value: String?): MediaType =
      entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: UNKNOWN
  }
}
