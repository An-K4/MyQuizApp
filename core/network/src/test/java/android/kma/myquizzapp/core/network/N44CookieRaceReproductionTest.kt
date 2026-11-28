package android.kma.myquizzapp.core.network

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.api.AuthApiService
import android.kma.myquizzapp.core.network.cookie.CredentialEpoch
import android.kma.myquizzapp.core.network.cookie.CredentialLifecycle
import android.kma.myquizzapp.core.network.cookie.RefreshVersion
import android.kma.myquizzapp.core.network.cookie.CredentialStampInterceptor
import android.kma.myquizzapp.core.network.cookie.CredentialCookieInterceptor
import okhttp3.CookieJar
import java.io.IOException
import android.kma.myquizzapp.core.network.cookie.TokenAuthenticator
import android.kma.myquizzapp.core.network.dto.AuthDataDto
import android.kma.myquizzapp.core.network.dto.GoogleOneTapRequest
import android.kma.myquizzapp.core.network.dto.LoginRequest
import android.kma.myquizzapp.core.network.dto.RegisterRequest
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Regression tests for cookie/session races, not an E2E backend check.
 * All cookie values below are synthetic fixtures, never user credentials.
 */
class N44CookieRaceReproductionTest {
    @Test
    fun `stale terminal refresh must not clear credentials published by newer login`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val store = RaceCookieStore()
        val host = "api.example.test"
        val auth = RaceAuth {
            started.countDown()
            check(release.await(5, TimeUnit.SECONDS)) { "Diagnostic latch timed out" }
            Result.Error(AppError.Api("AUTH_REFRESH_INVALID"))
        }
        val lifecycle = CredentialLifecycle(store)
        val oldEpoch = lifecycle.current()
        val authenticator = TokenAuthenticator(Lazy { auth }, lifecycle)
        val response =
            Response.Builder().request(Request.Builder().url("https://$host/v1/users/me").tag(CredentialEpoch::class.java, oldEpoch).build())
                .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
        val worker = Executors.newSingleThreadExecutor()
        try {
            val result = worker.submit(Callable {
                try { authenticator.authenticate(null, response) } catch (_: IOException) { null }
            })
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val fresh = lifecycle.beginFreshAuthentication { }
            lifecycle.save(fresh, host, listOf(fixtureCookie(host, "new-login-fixture")))
            release.countDown()
            result.get(10, TimeUnit.SECONDS)
            assertEquals(
                "New login cookie must survive an old refresh failure",
                "new-login-fixture", runBlocking { store.loadForHost(host).singleOrNull()?.value })
        } finally {
            release.countDown()
            worker.shutdownNow()
        }
    }

    @Test
    fun `concurrent 401 responses share one successful refresh`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val secondStarted = CountDownLatch(1)
        val lifecycle = CredentialLifecycle(RaceCookieStore())
        val epoch = lifecycle.current()
        val version = checkNotNull(lifecycle.refreshVersion(epoch))
        val auth = RaceAuth {
            started.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            Result.Success(Unit)
        }
        val authenticator = TokenAuthenticator(Lazy { auth }, lifecycle)
        val request = Request.Builder().url("https://api.example.test/v1/users/me")
            .tag(CredentialEpoch::class.java, epoch).tag(RefreshVersion::class.java, version).build()
        val response = Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
            .code(401).message("Unauthorized").build()
        val workers = Executors.newFixedThreadPool(2)
        try {
            val first = workers.submit(Callable { authenticator.authenticate(null, response) })
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val second = workers.submit(Callable {
                secondStarted.countDown()
                authenticator.authenticate(null, response)
            })
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS))
            release.countDown()
            assertEquals(request, first.get(10, TimeUnit.SECONDS))
            assertEquals(request, second.get(10, TimeUnit.SECONDS))
            assertEquals(1, auth.calls.get())
        } finally {
            release.countDown()
            workers.shutdownNow()
        }
    }

    @Test
    fun `late refresh Set Cookie must not resurrect credentials after local logout`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = MockWebServer()
        val store = RaceCookieStore()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                started.countDown()
                check(release.await(5, TimeUnit.SECONDS)) { "Diagnostic latch timed out" }
                return MockResponse().setHeader(
                    "Set-Cookie",
                    "session=old-refresh-fixture; Path=/; HttpOnly"
                )
                    .setBody("{}")
            }
        }
        server.start()
        val lifecycle = CredentialLifecycle(store)
        val oldEpoch = lifecycle.current()
        val client = OkHttpClient.Builder().cookieJar(CookieJar.NO_COOKIES)
            .addInterceptor(CredentialStampInterceptor(lifecycle))
            .addNetworkInterceptor(CredentialCookieInterceptor(lifecycle))
            .callTimeout(10, TimeUnit.SECONDS).build()
        val worker = Executors.newSingleThreadExecutor()
        try {
            val result = worker.submit(Callable {
                try {
                    client.newCall(Request.Builder().url(server.url("/v1/auth/refresh")).build())
                        .execute().use { it.body?.string() }
                } catch (_: IOException) { null }
            })
            assertTrue(started.await(5, TimeUnit.SECONDS))
            assertTrue(lifecycle.retireAndClear(oldEpoch)) // logout/deactivation cleanup boundary
            release.countDown()
            result.get(10, TimeUnit.SECONDS)
            assertTrue(
                "Retired cookie store must remain empty after the late HTTP response",
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
    override suspend fun loadForHost(host: String): List<StoredCookie> =
        synchronized(this) { byHost[host].orEmpty().toList() }

    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) {
        synchronized(this) {
            val names = cookies.map { it.name }.toSet()
            byHost[host] = byHost[host].orEmpty().filterNot { it.name in names } + cookies
        }
    }

    override suspend fun clear() {
        synchronized(this) { byHost.clear() }
    }
}

private fun fixtureCookie(host: String, value: String) =
    StoredCookie("session", value, host, "/", Long.MAX_VALUE, false, true)

private class RaceAuth(private val action: suspend () -> Result<Unit>) : AuthApiService {
    val calls = java.util.concurrent.atomic.AtomicInteger()
    override suspend fun refresh(epoch: CredentialEpoch?): Result<Unit> {
        calls.incrementAndGet()
        return action()
    }
    override suspend fun login(body: LoginRequest, epoch: CredentialEpoch?): Result<AuthDataDto> = error("unused")
    override suspend fun register(body: RegisterRequest, epoch: CredentialEpoch?): Result<AuthDataDto> = error("unused")
    override suspend fun loginWithGoogle(body: GoogleOneTapRequest, epoch: CredentialEpoch?): Result<AuthDataDto> =
        error("unused")

    override suspend fun logout(epoch: CredentialEpoch?): Result<Unit> = error("unused")
}
