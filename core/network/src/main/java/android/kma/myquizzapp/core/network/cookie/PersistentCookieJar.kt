package android.kma.myquizzapp.core.network.cookie

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistentCookieJar @Inject constructor(
    private val cookieStore: CookieStore
) : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        runBlocking { cookieStore.saveAll(url.host, cookies.map { it.toStoredCookie() }) }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = runBlocking {
        cookieStore.loadForHost(url.host).map { it.toOkHttpCookie() }
    }
}

private fun Cookie.toStoredCookie() =
    StoredCookie(name, value, domain, path, expiresAt, secure, httpOnly)

private fun StoredCookie.toOkHttpCookie(): Cookie = Cookie.Builder()
    .name(name).value(value).path(path).expiresAt(expiresAt).domain(domain)
    .apply {
        if (secure) secure()
        if (httpOnly) httpOnly()
    }
    .build()