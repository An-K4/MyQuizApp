package android.kma.myquizzapp.core.network.cookie

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BackgroundLogoutDispatcherTest {
    @Test fun `dispatch does not await response and late response cannot clear new cookies`() {
        val server = MockWebServer()
        val arrived = CountDownLatch(1)
        val release = CountDownLatch(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                arrived.countDown()
                check(release.await(10, TimeUnit.SECONDS))
                return MockResponse().addHeader("Set-Cookie", "accessToken=; Path=/; Max-Age=0")
            }
        }
        server.start()
        val completed = CountDownLatch(1)
        val client = OkHttpClient.Builder().eventListener(object : EventListener() {
            override fun callEnd(call: Call) { completed.countDown() }
        }).build()
        val store = LogoutTestStore()
        val lifecycle = CredentialLifecycle(store)
        val url = server.url("/v1/auth/logout")
        try {
            val old = lifecycle.current()
            lifecycle.save(old, url.host, listOf(cookie("accessToken", "old-access", url.host), cookie("refreshToken", "old-refresh", url.host)))
            BackgroundLogoutDispatcher(lifecycle, client, url).dispatch(old)
            assertTrue(arrived.await(5, TimeUnit.SECONDS))
            assertTrue(lifecycle.retireAndClear(old))
            val fresh = lifecycle.beginNewLifetime()
            lifecycle.save(fresh, url.host, listOf(cookie("accessToken", "new-access", url.host)))
            val sent = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("POST", sent.method)
            assertEquals("/v1/auth/logout", sent.path)
            assertEquals("accessToken=old-access; refreshToken=old-refresh", sent.getHeader("Cookie"))
            release.countDown()
            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals("new-access", lifecycle.load(fresh, url.host)!!.single().value)
        } finally {
            release.countDown()
            server.shutdown()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    @Test fun `stale lifetime or missing token never enqueues logout`() {
        val server = MockWebServer()
        server.start()
        val client = OkHttpClient()
        val lifecycle = CredentialLifecycle(LogoutTestStore())
        val url = server.url("/v1/auth/logout")
        try {
            val old = lifecycle.current()
            lifecycle.save(old, url.host, listOf(cookie("accessToken", "old-access", url.host)))
            val dispatcher = BackgroundLogoutDispatcher(lifecycle, client, url)
            dispatcher.dispatch(old)
            lifecycle.beginNewLifetime()
            dispatcher.dispatch(old)
            assertNull(server.takeRequest(300, TimeUnit.MILLISECONDS))
        } finally {
            server.shutdown()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    private fun cookie(name: String, value: String, host: String) = StoredCookie(
        name, value, host, "/", System.currentTimeMillis() + 60_000, false, true
    )
}

private class LogoutTestStore : CookieStore {
    private var cookies = emptyList<StoredCookie>()
    override suspend fun loadForHost(host: String): List<StoredCookie> = cookies
    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) { this.cookies = cookies }
    override suspend fun clear() { cookies = emptyList() }
}
