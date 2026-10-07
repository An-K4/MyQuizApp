package android.kma.myquizzapp.core.network

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.api.UserMutationApiService
import android.kma.myquizzapp.core.network.di.NetworkModule
import android.kma.myquizzapp.core.network.dto.ChangePasswordDto
import android.kma.myquizzapp.core.network.dto.DeactivateAccountDto
import android.kma.myquizzapp.core.network.result.ResultCallAdapterFactory
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.create

class AccountSecurityApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: UserMutationApiService
    private val json = NetworkModule.providePreserveCaseJson()
    @Before fun setup() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder().baseUrl(server.url("/v1/"))
            .addCallAdapterFactory(ResultCallAdapterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create()
    }
    @After fun teardown() = server.shutdown()
    private fun success() = MockResponse().setHeader("Content-Type", "application/json")
        .setBody("""{"success":true,"data":{"message":"Mutation completed"},"error":null}""")
    @Test fun `password uses exact camel case and success message unwraps to Unit`() = runTest {
        server.enqueue(success())
        assertTrue(api.changePassword(ChangePasswordDto(" oldpass", " newpass")) is Result.Success)
        val req = server.takeRequest(); assertEquals("PATCH", req.method); assertEquals("/v1/users/me/password", req.path)
        val body = json.parseToJsonElement(req.body.readUtf8()).jsonObject
        assertEquals(setOf("oldPassword", "newPassword"), body.keys)
        assertEquals(" oldpass", body.getValue("oldPassword").jsonPrimitive.content)
    }
    @Test fun `delete sends a real JSON body`() = runTest {
        server.enqueue(success()); assertTrue(api.deactivateAccount(DeactivateAccountDto("oldpass1")) is Result.Success)
        val req = server.takeRequest(); assertEquals("DELETE", req.method); assertEquals("/v1/users/me", req.path)
        assertEquals("oldpass1", json.parseToJsonElement(req.body.readUtf8()).jsonObject.getValue("password").jsonPrimitive.content)
    }
    @Test fun `business code survives both 400 and 403`() = runTest {
        for (status in listOf(400, 403)) {
            server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
                .setBody("""{"success":false,"data":null,"error":{"code":"USER_PASSWORD_INCORRECT"}}"""))
            assertEquals(AppError.Api("USER_PASSWORD_INCORRECT"), (api.deactivateAccount(DeactivateAccountDto("oldpass1")) as Result.Error).error)
        }
    }
    @Test fun `credential DTO debug strings are redacted`() {
        assertFalse(ChangePasswordDto("never-log-me", "never-log-me").toString().contains("never-log-me"))
        assertFalse(DeactivateAccountDto("never-log-me").toString().contains("never-log-me"))
    }
}
