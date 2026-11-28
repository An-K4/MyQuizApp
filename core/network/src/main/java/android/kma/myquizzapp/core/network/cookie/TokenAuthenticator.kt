package android.kma.myquizzapp.core.network.cookie

import android.kma.myquizzapp.core.common.cookie.CredentialStorageException
import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.hasApiCode
import android.kma.myquizzapp.core.common.error.isSessionTerminal
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.api.AuthApiService
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val authApi: Lazy<AuthApiService>,   // Lazy phá vòng lặp DI: Retrofit → OkHttp → Authenticator → Retrofit
    private val lifecycle: CredentialLifecycle
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 401 từ chính auth endpoints (login/refresh...) = credential sai → để UI xử lý, không retry
        if (response.request.url.encodedPath.contains("/auth/")) return null
        // Đã retry 1 lần mà vẫn 401 → bỏ, tránh vòng lặp vô hạn
        if (responseCount(response) >= 2) return null

        val epoch = response.request.tag(CredentialEpoch::class.java) ?: return null
        synchronized(this) {
            if (!lifecycle.isCurrent(epoch)) throw IOException("Credential lifetime retired")
            val requestVersion = response.request.tag(RefreshVersion::class.java)
            if (requestVersion != null && requestVersion != lifecycle.refreshVersion(epoch)) {
                return response.request
            }
            val refreshResult: Result<Unit> = runBlocking { authApi.get().refresh(epoch) }
            if (!lifecycle.isCurrent(epoch)) throw IOException("Credential lifetime retired")
            return when (refreshResult) {
                is Result.Success -> {
                    if (!lifecycle.markRefreshed(epoch)) throw IOException("Credential lifetime retired")
                    response.request
                }
                is Result.Error -> {
                    if (refreshResult.error.isSessionTerminal || refreshResult.error.hasApiCode("USER_NOT_FOUND")) {
                        lifecycle.retireAndClear(epoch)
                        null
                    } else if (refreshResult.error == AppError.LocalStorage) {
                        throw CredentialStorageException(IOException("Credential storage unavailable during refresh"))
                    } else {
                        // Propagate a transport failure, not the original 401; keep cookies/session.
                        throw IOException("Session refresh temporarily unavailable")
                    }
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}