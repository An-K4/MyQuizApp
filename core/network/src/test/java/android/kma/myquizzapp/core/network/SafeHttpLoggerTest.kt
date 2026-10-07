package android.kma.myquizzapp.core.network

import android.kma.myquizzapp.core.network.di.SafeHttpLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class SafeHttpLoggerTest {
    @Test fun `logger never writes credential bodies headers or query tokens`() {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody("response-secret"))
            val lines = mutableListOf<String>()
            val client = OkHttpClient.Builder().addInterceptor(SafeHttpLogger(true) { lines += it }).build()
            val request = Request.Builder().url(server.url("/v1/users/me/password?ticket=query-secret"))
                .header("Authorization", "header-secret")
                .post("body-secret".toRequestBody("application/json".toMediaType())).build()
            client.newCall(request).execute().close()
            assertEquals(2, lines.size)
            assertTrue(lines.first().contains("/v1/users/me/password"))
            assertFalse(lines.joinToString().contains("secret"))
        } finally { server.shutdown() }
    }
}
