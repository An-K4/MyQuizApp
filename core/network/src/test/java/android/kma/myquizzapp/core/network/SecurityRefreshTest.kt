package android.kma.myquizzapp.core.network

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.api.AuthApiService
import android.kma.myquizzapp.core.network.cookie.CredentialEpoch
import android.kma.myquizzapp.core.network.cookie.CredentialLifecycle
import android.kma.myquizzapp.core.network.cookie.TokenAuthenticator
import android.kma.myquizzapp.core.network.dto.AuthDataDto
import android.kma.myquizzapp.core.network.dto.GoogleOneTapRequest
import android.kma.myquizzapp.core.network.dto.LoginRequest
import android.kma.myquizzapp.core.network.dto.RegisterRequest
import dagger.Lazy
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class SecurityRefreshTest {
    private fun rejected(path: String = "users/me") = Response.Builder()
        .request(Request.Builder().url("https://api.example.test/v1/$path").tag(CredentialEpoch::class.java, CredentialEpoch(0)).build())
        .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()

    @Test
    fun `temporary refresh failure does not clear cookies or return terminal 401`() {
        for (error in listOf(
            AppError.Network,
            AppError.Server(500),
            AppError.Api("RATE_LIMITED")
        )) {
            val cookies = RefreshTestCookies()
            val auth = RefreshTestApi(Result.Error(error))
            try {
                TokenAuthenticator(Lazy { auth }, CredentialLifecycle(cookies)).authenticate(null, rejected())
                fail("Transient refresh must become a transport failure")
            } catch (_: IOException) {
            }
            assertEquals(0, cookies.clears)
        }
    }

    @Test
    fun `terminal refresh failure clears local credentials`() {
        val cookies = RefreshTestCookies()
        val auth = RefreshTestApi(Result.Error(AppError.Api("AUTH_REFRESH_INVALID")))
        assertNull(TokenAuthenticator(Lazy { auth }, CredentialLifecycle(cookies)).authenticate(null, rejected()))
        assertEquals(1, cookies.clears)
    }

    @Test
    fun `auth endpoints never trigger refresh`() {
        val cookies = RefreshTestCookies();
        val auth = RefreshTestApi(Result.Success(Unit))
        assertNull(
            TokenAuthenticator(Lazy { auth }, CredentialLifecycle(cookies)).authenticate(
                null,
                rejected("auth/login")
            )
        )
        assertEquals(0, auth.calls)
    }
}

private class RefreshTestCookies : CookieStore {
    var clears = 0
    override suspend fun loadForHost(host: String) = emptyList<StoredCookie>()
    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) = Unit
    override suspend fun clear() {
        clears++
    }
}

private class RefreshTestApi(private val result: Result<Unit>) : AuthApiService {
    var calls = 0
    override suspend fun refresh(epoch: CredentialEpoch?): Result<Unit> {
        calls++; return result
    }

    override suspend fun login(body: LoginRequest, epoch: CredentialEpoch?): Result<AuthDataDto> = error("unused")
    override suspend fun register(body: RegisterRequest, epoch: CredentialEpoch?): Result<AuthDataDto> = error("unused")
    override suspend fun loginWithGoogle(body: GoogleOneTapRequest, epoch: CredentialEpoch?): Result<AuthDataDto> =
        error("unused")

    override suspend fun logout(epoch: CredentialEpoch?): Result<Unit> = error("unused")
}
