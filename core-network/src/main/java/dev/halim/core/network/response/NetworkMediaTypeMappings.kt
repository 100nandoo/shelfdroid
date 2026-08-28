package dev.halim.core.network.response

import dev.halim.shelfdroid.core.MediaType

fun NetworkMediaType.toDomain(): MediaType =
  when (this) {
    NetworkMediaType.BOOK -> MediaType.BOOK
    NetworkMediaType.PODCAST -> MediaType.PODCAST
    NetworkMediaType.UNKNOWN -> MediaType.UNKNOWN
  }
