package dev.halim.core.network.client

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

@Singleton
class SessionCookieJar @Inject constructor() : CookieJar {

  private val cookies = mutableListOf<Cookie>()

  override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
    synchronized(this) {
      dropExpiredLocked()
      cookies.forEach { newCookie ->
        this.cookies.removeAll { existing ->
          existing.name == newCookie.name &&
            existing.domain == newCookie.domain &&
            existing.path == newCookie.path
        }
        this.cookies += newCookie
      }
    }
  }

  override fun loadForRequest(url: HttpUrl): List<Cookie> {
    synchronized(this) {
      dropExpiredLocked()
      return cookies.filter { it.matches(url) }
    }
  }

  private fun dropExpiredLocked() {
    val nowMillis = System.currentTimeMillis()
    cookies.removeAll { it.expiresAt <= nowMillis }
  }
}
