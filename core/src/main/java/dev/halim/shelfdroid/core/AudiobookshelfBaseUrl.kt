package dev.halim.shelfdroid.core

import java.net.URI

class AudiobookshelfBaseUrl
private constructor(
  val scheme: String,
  val host: String,
  val port: Int,
  val pathPrefix: String,
) {
  val origin: String = buildString {
    append(scheme)
    append("://")
    append(host)
    if (port != -1) append(":$port")
  }

  val value: String = if (pathPrefix.isEmpty()) origin else "$origin$pathPrefix"

  fun resolve(path: String, query: String? = null): String {
    val pathWithoutFragment = path.substringBefore("#")
    val rawPath = pathWithoutFragment.substringBefore("?").ifBlank { "/" }
    val existingQuery = pathWithoutFragment.substringAfter("?", "").takeIf { it.isNotBlank() }
    val mergedQuery =
      listOfNotNull(existingQuery, query).joinToString("&").takeIf { it.isNotBlank() }
    val resolvedPath = joinPath(pathPrefix, rawPath)
    return URI(scheme, null, host, port, resolvedPath, mergedQuery, null).toASCIIString()
  }

  fun socketPath(): String = joinPath(pathPrefix, "/socket.io")

  companion object {
    const val DEFAULT_VALUE = "https://audiobooks.dev/"
    val DEFAULT: AudiobookshelfBaseUrl = checkNotNull(parse(DEFAULT_VALUE))

    fun parse(raw: String): AudiobookshelfBaseUrl? {
      val trimmed = raw.trim()
      if (trimmed.isBlank()) return null

      val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
      val uri = runCatching { URI(candidate).normalize() }.getOrNull() ?: return null
      val scheme = uri.scheme?.lowercase() ?: return null
      if (scheme != "http" && scheme != "https") return null
      if (uri.userInfo != null) return null

      val host = uri.host?.lowercase() ?: return null
      val pathPrefix = normalizePathPrefix(uri.rawPath)
      return AudiobookshelfBaseUrl(
        scheme = scheme,
        host = host,
        port = uri.port,
        pathPrefix = pathPrefix,
      )
    }
  }
}

private fun normalizePathPrefix(rawPath: String?): String {
  val path = rawPath.orEmpty().replace(Regex("/{2,}"), "/")
  if (path.isBlank() || path == "/") return ""
  val trimmed = path.removeSuffix("/")
  return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
}

private fun joinPath(prefix: String, path: String): String {
  val normalizedPath = if (path.startsWith("/")) path else "/$path"
  if (prefix.isEmpty()) return normalizedPath
  return if (normalizedPath == "/") prefix else prefix + normalizedPath
}
