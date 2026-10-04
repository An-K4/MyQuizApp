package android.kma.myquizzapp.core.network.result

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.result.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

class ResultCallAdapterTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TestApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(ResultCallAdapterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TestApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `backend error code wins over HTTP status`() {
        listOf(
            401 to "AUTH_TOKEN_INVALID",
            403 to "GAME_FORBIDDEN",
            404 to "QUIZ_NOT_FOUND",
            410 to "USER_DEACTIVATED",
            503 to "SERVICE_UNAVAILABLE"
        ).forEach { (status, code) ->
            server.enqueue(errorResponse(status, code))
            val result = api.get().execute().body()
            assertEquals(AppError.Api(code), (result as Result.Error).error)
        }
    }

    @Test
    fun `malformed body falls back to HTTP classification`() {
        server.enqueue(MockResponse().setResponseCode(410).setBody("not-json"))
        val result = api.get().execute().body()
        assertTrue((result as Result.Error).error is AppError.Gone)
    }

    private fun errorResponse(status: Int, code: String) = MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody("""{"success":false,"data":null,"error":{"code":"$code"},"meta":null}""")

    private interface TestApi {
        @GET("test")
        fun get(): Call<Result<TestPayload>>
    }

    @Serializable
    private data class TestPayload(val id: Long)
}
