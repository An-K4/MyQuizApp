package android.kma.myquizzapp.core.network

import android.kma.myquizzapp.core.network.di.SafeHttpLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException

class SafeHttpLoggerTest {
    @Test
    fun `disabled logger produces no output`() {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse())
            val lines = mutableListOf<String>()
            val client = OkHttpClient.Builder().addInterceptor(SafeHttpLogger(false) { lines += it }).build()
            client.newCall(Request.Builder().url(server.url("/secret")).build()).execute().close()
            assertEquals(emptyList<String>(), lines)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `logger never writes exception messages`() {
        val lines = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(SafeHttpLogger(true) { lines += it })
            .addInterceptor { throw IOException("exception-secret") }
            .build()
        try {
            client.newCall(Request.Builder().url("https://example.invalid/path-secret").build()).execute()
            throw AssertionError("Expected IOException")
        } catch (_: IOException) {
            assertEquals(listOf("HTTP GET"), lines)
        }
    }

    @Test
    fun `logger never writes credentials in path body headers query or response`() {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody("response-secret"))
            val lines = mutableListOf<String>()
            val client =
                OkHttpClient.Builder().addInterceptor(SafeHttpLogger(true) { lines += it }).build()
            val request =
                Request.Builder().url(server.url("/v1/users/path-secret?ticket=query-secret"))
                    .header("Authorization", "header-secret")
                    .post("body-secret".toRequestBody("application/json".toMediaType())).build()
            client.newCall(request).execute().close()
            assertEquals(2, lines.size)
            assertEquals("HTTP POST", lines.first())
            assertEquals("HTTP POST -> 200", lines.last())
            assertFalse(lines.joinToString().contains("secret"))
        } finally {
            server.shutdown()
        }
    }
}
