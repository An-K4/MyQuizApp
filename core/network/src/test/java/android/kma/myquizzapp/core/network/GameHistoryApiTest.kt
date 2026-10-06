package android.kma.myquizzapp.core.network

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.api.GameApiService
import android.kma.myquizzapp.core.network.di.NetworkModule
import android.kma.myquizzapp.core.network.result.ResultCallAdapterFactory
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.create

class GameHistoryApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: GameApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = NetworkModule.providePreserveCaseJson()
        api = Retrofit.Builder()
            .baseUrl(server.url("/v1/"))
            .addCallAdapterFactory(ResultCallAdapterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `guest history sends header query and exposes cursor page`() = runTest {
        server.enqueue(successResponse())

        val result = api.getGameHistory(
            role = "played",
            cursor = "cursor-1",
            limit = 20,
            includeTotal = false,
            guestId = "6f1c1f8e-0b2a-4f1d-9a3e-0c7c9b2d5e11"
        )
        val request = server.takeRequest()

        assertEquals("played", request.requestUrl?.queryParameter("role"))
        assertEquals("cursor-1", request.requestUrl?.queryParameter("cursor"))
        assertEquals("20", request.requestUrl?.queryParameter("limit"))
        assertEquals("false", request.requestUrl?.queryParameter("include_total"))
        assertEquals("6f1c1f8e-0b2a-4f1d-9a3e-0c7c9b2d5e11", request.getHeader("x-guest-id"))
        assertTrue(result is Result.Success)
        result as Result.Success
        assertEquals("next-2", result.page?.nextCursor)
        assertTrue(result.page?.hasMore == true)
    }

    @Test
    fun `account history omits guest header`() = runTest {
        server.enqueue(successResponse())

        api.getGameHistory(role = "hosted")
        val request = server.takeRequest()

        assertEquals("hosted", request.requestUrl?.queryParameter("role"))
        assertNull(request.getHeader("x-guest-id"))
    }

    private fun successResponse() = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(
            """{"success":true,"data":{"sessions":[]},"meta":{"pagination":{"limit":20,"nextCursor":"next-2","hasMore":true}}}"""
        )
}
