package android.kma.myquizzapp.core.network

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import android.kma.myquizzapp.core.network.cookie.PersistentCookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic values only; storage here deliberately returns candidates from every domain. */
class PersistentCookieScopeTest {
    private val store = FakeCookieStore()
    private val jar = PersistentCookieJar(store)
    private val origin = "https://api.example.test/v1/auth/login".toHttpUrl()

    private fun set(header: String) {
        jar.saveFromResponse(origin, listOf(requireNotNull(Cookie.parse(origin, header))))
    }

    @Test fun `host-only cookie does not leak to subdomain`() {
        set("session=synthetic; Path=/; Secure")
        assertEquals(1, jar.loadForRequest("https://api.example.test/".toHttpUrl()).size)
        assertTrue(jar.loadForRequest("https://child.api.example.test/".toHttpUrl()).isEmpty())
    }

    @Test fun `domain cookie matches subdomains but not unrelated hosts`() {
        set("session=synthetic; Domain=example.test; Path=/")
        assertEquals(1, jar.loadForRequest("https://other.example.test/".toHttpUrl()).size)
        assertTrue(jar.loadForRequest("https://notexample.test/".toHttpUrl()).isEmpty())
    }

    @Test fun `same name paths coexist and secure path rules apply`() {
        set("session=root; Path=/")
        set("session=private; Path=/v1; Secure")
        assertEquals(listOf("private", "root"), jar.loadForRequest(origin).map { it.value })
        assertEquals(listOf("root"), jar.loadForRequest("http://api.example.test/v1".toHttpUrl()).map { it.value })
        assertEquals(listOf("root"), jar.loadForRequest("https://api.example.test/v10".toHttpUrl()).map { it.value })
    }

    @Test fun `expired Set Cookie removes only matching path`() {
        set("session=root; Path=/")
        set("session=private; Path=/v1")
        set("session=gone; Path=/v1; Max-Age=0")
        assertEquals(listOf("root"), jar.loadForRequest(origin).map { it.value })
    }
}

private class FakeCookieStore : CookieStore {
    private val cookies = linkedMapOf<Triple<String, String, String>, StoredCookie>()
    override suspend fun loadForHost(host: String) = cookies.values.filter { it.expiresAt > System.currentTimeMillis() }
    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) {
        cookies.forEach {
            val key = Triple(it.domain, it.path, it.name)
            if (it.expiresAt <= System.currentTimeMillis()) this.cookies.remove(key) else this.cookies[key] = it
        }
    }
    override suspend fun clear() = cookies.clear()
}
