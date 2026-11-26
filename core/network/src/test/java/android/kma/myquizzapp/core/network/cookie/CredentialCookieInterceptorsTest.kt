package android.kma.myquizzapp.core.network.cookie

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CredentialCookieInterceptorsTest {
    @Test fun `retired response cannot write after logout`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = MockWebServer()
        val store = InterceptorTestStore()
        val lifecycle = CredentialLifecycle(store)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                started.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                return MockResponse().addHeader("Set-Cookie", "session=late-fixture; Path=/")
            }
        }
        server.start()
        val client = client(lifecycle)
        val worker = Executors.newSingleThreadExecutor()
        try {
            val epoch = lifecycle.current()
            val response = worker.submit(Callable {
                try {
                    client.newCall(Request.Builder().url(server.url("/auth/refresh")).build())
                        .execute().close()
                    false
                } catch (_: IOException) { true }
            })
            assertTrue(started.await(5, TimeUnit.SECONDS))
            assertTrue(lifecycle.retireAndClear(epoch))
            release.countDown()
            assertTrue(response.get(10, TimeUnit.SECONDS))
            assertTrue(lifecycle.load(lifecycle.current(), server.hostName).orEmpty().isEmpty())
        } finally {
            release.countDown()
            worker.shutdownNow()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }

    @Test fun `stale expired cookie cannot erase newer login`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = MockWebServer()
        val lifecycle = CredentialLifecycle(InterceptorTestStore())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/old" -> {
                    started.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    MockResponse().addHeader("Set-Cookie", "session=; Path=/; Max-Age=0")
                }
                else -> MockResponse().addHeader("Set-Cookie", "session=new-fixture; Path=/")
            }
        }
        server.start()
        val client = client(lifecycle)
        val worker = Executors.newSingleThreadExecutor()
        try {
            val old = worker.submit(Callable {
                try {
                    client.newCall(Request.Builder().url(server.url("/old")).build()).execute().close()
                    false
                } catch (_: IOException) { true }
            })
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val fresh = lifecycle.beginNewLifetime()
            client.newCall(Request.Builder().url(server.url("/login")).build()).execute().close()
            release.countDown()
            assertTrue(old.get(10, TimeUnit.SECONDS))
            assertEquals("new-fixture", lifecycle.load(fresh, server.hostName)?.single()?.value)
        } finally {
            release.countDown()
            worker.shutdownNow()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }

    @Test fun `guest request cannot refresh while a new login is pending`() {
        val server = MockWebServer()
        server.start()
        val lifecycle = CredentialLifecycle(InterceptorTestStore())
        val client = client(lifecycle)
        try {
            val epoch = lifecycle.beginFreshAuthentication { }
            try {
                client.newCall(Request.Builder().url(server.url("/users/me")).build()).execute().close()
                throw AssertionError("Pending login must block unrelated request")
            } catch (_: IOException) { }
            assertEquals(0, server.requestCount)
            server.enqueue(MockResponse().addHeader("Set-Cookie", "session=new-fixture; Path=/"))
            client.newCall(Request.Builder().url(server.url("/auth/login")).build()).execute().close()
            assertTrue(lifecycle.finishAuthentication(epoch))
            server.enqueue(MockResponse())
            client.newCall(Request.Builder().url(server.url("/users/me")).build()).execute().close()
            server.takeRequest()
            assertEquals("session=new-fixture", server.takeRequest().getHeader("Cookie"))
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }

    @Test fun `cookies obey path and secure scope`() {
        val server = MockWebServer()
        server.start()
        val lifecycle = CredentialLifecycle(InterceptorTestStore())
        val client = client(lifecycle)
        try {
            val host = server.hostName
            val expiry = System.currentTimeMillis() + 60_000
            lifecycle.save(lifecycle.current(), host, listOf(
                StoredCookie("sid", "root-fixture", host, "/", expiry, false, true),
                StoredCookie("sid", "secure-fixture", host, "/private", expiry, true, true),
            ))
            server.enqueue(MockResponse().setResponseCode(200))
            client.newCall(Request.Builder().url(server.url("/private")).build()).execute().close()
            assertEquals("sid=root-fixture", server.takeRequest().getHeader("Cookie"))
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }

    private fun client(lifecycle: CredentialLifecycle) = OkHttpClient.Builder()
        .cookieJar(CookieJar.NO_COOKIES)
        .addInterceptor(CredentialStampInterceptor(lifecycle))
        .addNetworkInterceptor(CredentialCookieInterceptor(lifecycle))
        .build()
}

private class InterceptorTestStore : CookieStore {
    private val cookies = mutableMapOf<Triple<String, String, String>, StoredCookie>()
    override suspend fun loadForHost(host: String) = cookies.values.filter {
        host == it.domain || host.endsWith(".${it.domain}")
    }
    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) {
        cookies.forEach {
            val key = Triple(it.name, it.domain, it.path)
            if (it.expiresAt <= System.currentTimeMillis()) this.cookies.remove(key)
            else this.cookies[key] = it
        }
    }
    override suspend fun clear() { cookies.clear() }
}
