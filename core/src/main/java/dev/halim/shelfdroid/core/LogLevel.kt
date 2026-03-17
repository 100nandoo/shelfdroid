package dev.halim.shelfdroid.core

enum class LogLevel(val value: Int) {
  DEBUG(1),
  INFO(2),
  WARNING(3),
  ERROR(4);

  companion object {
    fun from(value: Int): LogLevel {
      return when (value) {
        1 -> DEBUG
        2 -> INFO
        3 -> WARNING
        4 -> ERROR
        else -> DEBUG
      }
    }
  }
}
