package au.com.swagritech.opsapp.api

import java.util.concurrent.ConcurrentHashMap
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Shared in-memory cookie jar so EasyAuth login cookies can be reused by API calls.
 */
object SessionCookieJar : CookieJar {
    private val store: MutableMap<String, MutableList<Cookie>> = ConcurrentHashMap()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val host = url.host
        val existing = store[host]?.toMutableList() ?: mutableListOf()

        cookies.forEach { incoming ->
            existing.removeAll { it.name == incoming.name && it.matches(url) }
            existing.add(incoming)
        }

        store[host] = existing
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val existing = store[host] ?: return emptyList()
        val valid = existing.filter { it.matches(url) && !it.expiresAt.let { expiry -> expiry < System.currentTimeMillis() } }
        store[host] = valid.toMutableList()
        return valid
    }

    fun hasCookiesFor(url: HttpUrl): Boolean = loadForRequest(url).isNotEmpty()

    fun clear() {
        store.clear()
    }
}

