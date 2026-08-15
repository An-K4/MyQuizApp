package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.result.map
import android.kma.myquizzapp.core.network.api.AuthApiService
import android.kma.myquizzapp.core.network.api.UserApiService
import android.kma.myquizzapp.core.network.dto.ForgotPasswordRequest
import timber.log.Timber
import android.kma.myquizzapp.core.network.dto.GoogleOneTapRequest
import android.kma.myquizzapp.core.network.dto.LoginRequest
import android.kma.myquizzapp.core.network.dto.RegisterRequest
import android.kma.myquizzapp.core.network.dto.ResetPasswordRequest
import android.kma.myquizzapp.core.network.dto.ResetPasswordWithOtpRequest
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApiService,
    private val userApi: UserApiService,
    private val cookieStore: CookieStore, // interface ở core:common — dùng cho logout
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> =
        // Cookie đã được PersistentCookieJar.saveFromResponse() lưu vào Room
        // TRƯỚC khi dòng này chạy — repository không cần động vào token.
        authApi.login(LoginRequest(email, password)).map { it.user.toDomain() }

    override suspend fun register(
        email: String, password: String, fullname: String, phone: String?,
    ): Result<User> {
        // ⚠️ HỢP ĐỒNG QUAN TRỌNG: POST /auth/register KHÔNG set cookie (xác minh trong
        // auth.controller.ts — chỉ trả 201 + { user }). Đăng ký xong phải login ngay
        // để CookieJar nhận cookie, nếu không user sẽ bị hất ra màn Login.
        val reg = authApi.register(
            RegisterRequest(
                email,
                password,
                fullname,
                phone?.takeIf { it.isNotBlank() })
        )
        if (reg is Result.Error) return reg
        return login(email, password)
    }

    override suspend fun loginWithGoogle(idToken: String): Result<User> =
        authApi.loginWithGoogle(GoogleOneTapRequest(idToken)).map { it.user.toDomain() }

    override suspend fun getCurrentUser(): Result<User> =
        userApi.getMe().map { it.user.toDomain() }

    override suspend fun logout(): Result<Unit> {
        val result = authApi.logout() // server revoke refresh token + clearCookie
        // Dù call server thất bại (mất mạng...) vẫn dọn local — trạng thái "đã logout"
        // phía client là quan trọng nhất. CookieStore.clear() bạn đã thêm ở Tuần 1.
        cookieStore.clear()
        return result
    }

    override suspend fun isAuthenticated(): Boolean {
        // Verify with backend - if getCurrentUser succeeds, session is valid
        // If no cookies exist, API will return 401 and we return false
        return when (getCurrentUser()) {
            is Result.Success -> true
            is Result.Error -> false
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        Timber.d("Forgot Pass: Repository - Calling API forgotPassword with email: $email")
        val result = authApi.forgotPassword(ForgotPasswordRequest(email))
        when (result) {
            is Result.Success -> Timber.d("Forgot Pass: Repository - API returned SUCCESS")
            is Result.Error -> {
                Timber.e("Forgot Pass: Repository - API returned ERROR")
                Timber.e("Forgot Pass: Error details: ${result.error}")
            }
        }
        return result
    }

    override suspend fun resetPasswordWithToken(token: String, newPassword: String): Result<Unit> =
        authApi.resetPasswordWithToken(ResetPasswordRequest(token, newPassword))

    override suspend fun resetPasswordWithOtp(
        email: String,
        otp: String,
        newPassword: String
    ): Result<Unit> =
        authApi.resetPasswordWithOtp(ResetPasswordWithOtpRequest(email, otp, newPassword))
}