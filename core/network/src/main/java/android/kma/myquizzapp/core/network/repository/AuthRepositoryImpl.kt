package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.model.ResetTicket
import android.kma.myquizzapp.core.common.model.ResetTicketStatus
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.result.map
import android.kma.myquizzapp.core.network.cookie.CredentialEpoch
import android.kma.myquizzapp.core.network.cookie.CredentialLifecycle
import android.kma.myquizzapp.core.network.api.AuthApiService
import android.kma.myquizzapp.core.network.api.PasswordResetApiService
import android.kma.myquizzapp.core.network.api.UserApiService
import android.kma.myquizzapp.core.network.dto.CompleteResetRequest
import android.kma.myquizzapp.core.network.dto.ForgotPasswordRequest
import android.kma.myquizzapp.core.network.dto.GoogleOneTapRequest
import android.kma.myquizzapp.core.network.dto.LoginRequest
import android.kma.myquizzapp.core.network.dto.RegisterRequest
import android.kma.myquizzapp.core.network.dto.VerifyResetOtpRequest
import android.kma.myquizzapp.core.network.dto.VerifyResetTokenRequest
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApiService,
    private val userApi: UserApiService,
    private val passwordResetApi: PasswordResetApiService, // N16.5: PreserveCaseJson (module user dùng camelCase thật)
    private val lifecycle: CredentialLifecycle,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> =
        // The guarded network interceptor persists cookies before returning the response.
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

    override suspend fun getCurrentUserAtEpoch(epoch: Long): Result<User> =
        userApi.getMe(CredentialEpoch(epoch)).map { it.user.toDomain() }

    override suspend fun logout(): Result<Unit> {
        val epoch = lifecycle.current()
        val result = authApi.logout(epoch) // server revoke refresh token + clearCookie
        // Dù call server thất bại (mất mạng...) vẫn dọn local — trạng thái "đã logout"
        // phía client là quan trọng nhất. CookieStore.clear() bạn đã thêm ở Tuần 1.
        lifecycle.retireAndClear(epoch)
        return result
    }

    override suspend fun loginAtEpoch(email: String, password: String, epoch: Long): Result<User> =
        authApi.login(LoginRequest(email, password), CredentialEpoch(epoch)).map { it.user.toDomain() }

    override suspend fun registerAtEpoch(email: String, password: String, fullname: String, phone: String?, epoch: Long): Result<User> {
        val stamp = CredentialEpoch(epoch)
        val reg = authApi.register(RegisterRequest(email, password, fullname, phone?.takeIf { it.isNotBlank() }), stamp)
        if (reg is Result.Error) return reg
        return loginAtEpoch(email, password, epoch)
    }

    override suspend fun loginWithGoogleAtEpoch(idToken: String, epoch: Long): Result<User> =
        authApi.loginWithGoogle(GoogleOneTapRequest(idToken), CredentialEpoch(epoch)).map { it.user.toDomain() }

    override suspend fun logoutAtEpoch(epoch: Long): Result<Unit> = authApi.logout(CredentialEpoch(epoch))

    override suspend fun isAuthenticated(): Boolean {
        // Verify with backend - if getCurrentUser succeeds, session is valid
        // If no cookies exist, API will return 401 and we return false
        return when (getCurrentUser()) {
            is Result.Success -> true
            is Result.Error -> false
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> {
        // N16.5: response giờ có data { resetTime, expiresAt } — app không dùng tới
        // (đếm ngược 60s cố định ở màn OTP) nên map về Unit.
        return passwordResetApi.forgotPassword(ForgotPasswordRequest(email)).map { }
    }

    override suspend fun verifyResetWithOtp(email: String, otp: String): Result<ResetTicket> =
        passwordResetApi.verifyResetWithOtp(VerifyResetOtpRequest(email, otp)).map { it.toDomain() }

    override suspend fun verifyResetWithToken(token: String): Result<ResetTicket> =
        passwordResetApi.verifyResetWithToken(VerifyResetTokenRequest(token)).map { it.toDomain() }

    override suspend fun getResetTicket(ticket: String): Result<ResetTicketStatus> =
        passwordResetApi.getResetTicket(ticket).map { it.toDomain() }

    override suspend fun completeReset(ticket: String, newPassword: String): Result<Unit> =
        passwordResetApi.completeReset(CompleteResetRequest(ticket, newPassword)).map { }
}