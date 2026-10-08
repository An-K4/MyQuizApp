package android.kma.myquizzapp.core.network

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.api.AuthApiService
import android.kma.myquizzapp.core.network.cookie.PersistentCookieJar
import android.kma.myquizzapp.core.network.cookie.TokenAuthenticator
import android.kma.myquizzapp.core.network.dto.*
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Opt-in RED diagnostics, not passing regression tests or an E2E backend check.
 * Current transport is expected to violate these desired safety invariants.
 * Run with N44_COOKIE_RACE_DIAGNOSTICS=1, capture the failure, then design the fix.
 * Once fixed, remove opt-in and keep these as ordinary regression tests.
 * All cookie values below are synthetic fixtures, never user credentials.
 */
class N44CookieRaceReproductionTest {
    @Before fun optIn() {
        assumeTrue("N44 cookie race diagnostics are explicitly opt-in",
            System.getenv("N44_COOKIE_RACE_DIAGNOSTICS") == "1")
    }

    @Test fun `stale terminal refresh must not clear credentials published by newer login`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val store = RaceCookieStore()
        val host = "api.example.test"
        val auth = RaceAuth {
            started.countDown()
            check(release.await(5, TimeUnit.SECONDS)) { "Diagnostic latch timed out" }
            Result.Error(AppError.Api("AUTH_REFRESH_INVALID"))
        }
        val authenticator = TokenAuthenticator(Lazy { auth }, store)
        val response = Response.Builder().request(Request.Builder().url("https://$host/v1/users/me").build())
            .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
        val worker = Executors.newSingleThreadExecutor()
        try {
            val result = worker.submit(Callable { authenticator.authenticate(null, response) })
            assertTrue(started.await(5, TimeUnit.SECONDS))
            runBlocking { store.clear(); store.saveAll(host, listOf(fixtureCookie(host, "new-login-fixture"))) }
            release.countDown()
            result.get(10, TimeUnit.SECONDS)
            assertEquals("New login cookie must survive an old refresh failure",
                "new-login-fixture", runBlocking { store.loadForHost(host).singleOrNull()?.value })
        } finally {
            release.countDown()
            worker.shutdownNow()
        }
    }

    @Test fun `late refresh Set Cookie must not resurrect credentials after local logout`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = MockWebServer()
        val store = RaceCookieStore()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                started.countDown()
                check(release.await(5, TimeUnit.SECONDS)) { "Diagnostic latch timed out" }
                return MockResponse().setHeader("Set-Cookie", "session=old-refresh-fixture; Path=/; HttpOnly")
                    .setBody("{}")
            }
        }
        server.start()
        val client = OkHttpClient.Builder().cookieJar(PersistentCookieJar(store))
            .callTimeout(10, TimeUnit.SECONDS).build()
        val worker = Executors.newSingleThreadExecutor()
        try {
            val result = worker.submit(Callable {
                client.newCall(Request.Builder().url(server.url("/v1/auth/refresh")).build())
                    .execute().use { it.body?.string() }
            })
            assertTrue(started.await(5, TimeUnit.SECONDS))
            runBlocking { store.clear() } // current logout/deactivation cleanup boundary
            release.countDown()
            result.get(10, TimeUnit.SECONDS)
            assertTrue("Retired cookie store must remain empty after the late HTTP response",
                runBlocking { store.loadForHost(server.url("/").host).isEmpty() })
        } finally {
            release.countDown()
            worker.shutdownNow()
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            server.shutdown()
        }
    }
}

private class RaceCookieStore : CookieStore {
    private val byHost = mutableMapOf<String, List<StoredCookie>>()
    override suspend fun loadForHost(host: String): List<StoredCookie> = synchronized(this) { byHost[host].orEmpty().toList() }
    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) {
        synchronized(this) {
            val names = cookies.map { it.name }.toSet()
            byHost[host] = byHost[host].orEmpty().filterNot { it.name in names } + cookies
        }
    }
    override suspend fun clear() { synchronized(this) { byHost.clear() } }
}
private fun fixtureCookie(host: String, value: String) =
    StoredCookie("session", value, host, "/", Long.MAX_VALUE, false, true)

private class RaceAuth(private val action: suspend () -> Result<Unit>) : AuthApiService {
    override suspend fun refresh(): Result<Unit> = action()
    override suspend fun login(body: LoginRequest): Result<AuthDataDto> = error("unused")
    override suspend fun register(body: RegisterRequest): Result<AuthDataDto> = error("unused")
    override suspend fun loginWithGoogle(body: GoogleOneTapRequest): Result<AuthDataDto> = error("unused")
    override suspend fun logout(): Result<Unit> = error("unused")
}
