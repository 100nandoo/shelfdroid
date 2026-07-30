package dev.halim.core.network.client

internal object AnonymousRequest {
  const val HEADER_NAME = "x-shelfdroid-anonymous"
  const val HEADER_VALUE = "true"
}

internal data object AnonymousRequestTag
