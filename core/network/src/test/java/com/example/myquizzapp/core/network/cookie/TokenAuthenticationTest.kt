package com.example.myquizzapp.core.network.cookie

import com.example.myquizzapp.core.common.cookie.CookieStore
import com.example.myquizzapp.core.common.cookie.StoredCookie
import com.example.myquizzapp.core.network.api.AuthApiService
import com.example.myquizzapp.core.network.result.ResultCallAdapterFactory
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap

class TokenAuthenticatorTest {

    private val server = MockWebServer()
    private lateinit var cookieStore: FakeCookieStore
    private lateinit var client: OkHttpClient
    private val host: String get() = server.hostName

    @Before
    fun setUp() {
        server.start()
        cookieStore = FakeCookieStore()
        val cookieJar = PersistentCookieJar(cookieStore)

        // Lazy phá vòng lặp: Retrofit cần client → client cần authenticator → authenticator cần AuthApiService
        lateinit var authApi: AuthApiService
        val authenticator = TokenAuthenticator(Lazy { authApi }, cookieStore)

        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .authenticator(authenticator)
            .build()

        val json = Json { ignoreUnknownKeys = true }
        authApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)   // refresh đi qua cùng client → jar tự gắn/nhận cookie
            .addCallAdapterFactory(ResultCallAdapterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApiService::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `401 triggers refresh then retries original request with new cookie`() = runBlocking {
        seedOldSession()

        server.enqueue(MockResponse().setResponseCode(401))              // #1 request gốc fail
        server.enqueue(                                                  // #2 refresh OK + cookie mới
            MockResponse().setResponseCode(200)
                .addHeader("Set-Cookie", "accessToken=new-token; Path=/; HttpOnly")
                .setBody("""{"success":true,"data":{"message":"Tokens refreshed successfully"},"error":null}""")
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}")) // #3 retry thành công

        val response = client.newCall(
            Request.Builder().url(server.url("/games")).build()
        ).execute()

        assertEquals(200, response.code)
        assertEquals(3, server.requestCount)

        server.takeRequest()                                  // #1 /games (cookie cũ)
        val refreshReq = server.takeRequest()                 // #2 /auth/refresh — phải kèm refreshToken
        assertTrue(refreshReq.path!!.contains("auth/refresh"))
        assertTrue(refreshReq.getHeader("Cookie")!!.contains("refreshToken=refresh-token"))
        val retryReq = server.takeRequest()                   // #3 /games với cookie MỚI
        assertTrue(retryReq.getHeader("Cookie")!!.contains("accessToken=new-token"))
    }

    @Test
    fun `401 with dead refresh token clears cookies and gives up`() = runBlocking {
        seedOldSession()

        server.enqueue(MockResponse().setResponseCode(401))   // #1 request gốc
        server.enqueue(MockResponse().setResponseCode(401))   // #2 refresh cũng chết

        val response = client.newCall(
            Request.Builder().url(server.url("/games")).build()
        ).execute()

        assertEquals(401, response.code)                      // trả 401 gốc, không retry vô hạn
        assertEquals(2, server.requestCount)                  // chỉ 2 request: gốc + refresh
        assertTrue(cookieStore.loadForHost(host).isEmpty())                     // cookie đã bị clear → UI về Login
    }

    /** Giả lập đã login từ trước: cặp cookie cũ còn hạn */
    private suspend fun seedOldSession() {
        val expires = System.currentTimeMillis() + 3_600_000
        cookieStore.saveAll(
            host,
            listOf(
                StoredCookie("accessToken", "old-token", host, "/", expires, secure = false, httpOnly = true),
                StoredCookie("refreshToken", "refresh-token", host, "/", expires, secure = false, httpOnly = true)
            )
        )
    }
}

/** CookieStore in-memory — nhờ DIP mà test không cần đụng Room */
private class FakeCookieStore : CookieStore {
    private val map = ConcurrentHashMap<String, StoredCookie>()

    override suspend fun loadForHost(host: String): List<StoredCookie> =
        map.values.filter { it.domain == host && it.expiresAt > System.currentTimeMillis() }

    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) {
        cookies.forEach { map["$host|${it.name}"] = it }
    }

    override suspend fun clear() = map.clear()
}