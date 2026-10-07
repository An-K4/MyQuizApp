package android.kma.myquizzapp.core.network

import android.kma.myquizzapp.core.common.model.AuthProvider
import android.kma.myquizzapp.core.common.model.UserRole
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.api.UserMutationApiService
import android.kma.myquizzapp.core.network.di.NetworkModule
import android.kma.myquizzapp.core.network.dto.AuthDataDto
import android.kma.myquizzapp.core.network.dto.AvatarRequestDto
import android.kma.myquizzapp.core.network.dto.UserProfilePatchDto
import android.kma.myquizzapp.core.network.result.ResultCallAdapterFactory
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
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

class UserProfileApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: UserMutationApiService
    private val json = NetworkModule.providePreserveCaseJson()

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder().baseUrl(server.url("/v1/"))
            .addCallAdapterFactory(ResultCallAdapterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create()
    }
    @After fun tearDown() = server.shutdown()

    @Test fun `profile patch omits untouched fields and preserves explicit clear`() = runTest {
        server.enqueue(response("""{"user":$USER_JSON}"""))
        val result = api.updateProfile(UserProfilePatchDto(description = "", phone = ""))
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/v1/users/me", request.path)
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(setOf("phone", "description"), body.keys)
        assertEquals("", body.getValue("phone").jsonPrimitive.content)
        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data.user.toDomain()
        assertEquals(UserRole.ADMIN, user.role)
        assertEquals(AuthProvider.GOOGLE, user.authProvider)
        assertEquals("2026-10-06", user.updatedAt)
    }

    @Test fun `avatar endpoint keeps camel case on request and response`() = runTest {
        server.enqueue(response("""{"avatarUrl":"https://images.test/new"}"""))
        val result = api.updateAvatar(AvatarRequestDto("https://images.test/new"))
        val request = server.takeRequest()
        assertEquals("/v1/users/me/avatar", request.path)
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(setOf("fileUrl"), body.keys)
        assertEquals("https://images.test/new", body.getValue("fileUrl").jsonPrimitive.content)
        assertEquals("https://images.test/new", (result as Result.Success).data.avatarUrl)
    }

    @Test fun `user dto keeps provider role and timestamps under both json boundaries`() {
        for (decoder in listOf(json, NetworkModule.provideJson())) {
            val user = decoder.decodeFromString<AuthDataDto>("""{"user":$USER_JSON}""").user.toDomain()
            assertEquals(UserRole.ADMIN, user.role)
            assertEquals(AuthProvider.GOOGLE, user.authProvider)
            assertEquals("2026-10-01", user.createdAt)
            assertEquals("2026-10-06", user.updatedAt)
        }
    }

    private fun response(data: String) = MockResponse().setHeader("Content-Type", "application/json")
        .setBody("""{"success":true,"data":$data}""")

    companion object {
        private const val USER_JSON = """{"id":7,"email":"user@example.com","fullname":"User Name","role":"admin","auth_provider":"google","created_at":"2026-10-01","updated_at":"2026-10-06"}"""
    }
}
