package android.kma.myquizzapp.domain.profile

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.isSessionTerminal
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.repository.StorageRepository
import android.kma.myquizzapp.core.common.repository.UserRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

sealed interface AvatarUpdateResult {
    data object Saved : AvatarUpdateResult
    data object SessionChanged : AvatarUpdateResult
    data class Failed(val error: AppError) : AvatarUpdateResult
    data class NeedsVerification(val publicUrl: String, val token: SessionUserToken) : AvatarUpdateResult
}

/** Never replay avatar PATCH: backend deletes the previously stored file before updating DB. */
class UpdateAvatarUseCase @Inject constructor(
    private val storage: StorageRepository,
    private val users: UserRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(bytes: ByteArray, token: SessionUserToken): AvatarUpdateResult {
        if (!isCurrent(token)) return AvatarUpdateResult.SessionChanged
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return AvatarUpdateResult.Failed(AppError.Api("FILE_TOO_LARGE"))
        val presign = when (val result = storage.presignUpload("image/jpeg", "avatars", bytes.size.toLong())) {
            is Result.Success -> result.data
            is Result.Error -> return failed(result.error, token)
        }
        if (!isCurrent(token)) return AvatarUpdateResult.SessionChanged
        when (val result = storage.uploadBytes(presign.uploadUrl, "image/jpeg", bytes)) {
            is Result.Success -> Unit
            is Result.Error -> return failed(result.error, token)
        }
        if (!isCurrent(token)) return AvatarUpdateResult.SessionChanged
        return when (val result = users.updateAvatar(presign.publicUrl)) {
            is Result.Success -> if (session.applyAvatarUpdate(token, result.data)) AvatarUpdateResult.Saved
                else AvatarUpdateResult.SessionChanged
            is Result.Error -> {
                if (result.error.isSessionTerminal || !isUncertainWrite(result.error)) {
                    return failed(result.error, token)
                }
                // A timeout/5xx can follow a successful DB write. Reconcile, do not retry PATCH.
                verify(presign.publicUrl, token)
            }
        }
    }

    suspend fun verify(publicUrl: String, token: SessionUserToken): AvatarUpdateResult {
        if (!isCurrent(token)) return AvatarUpdateResult.SessionChanged
        return when (val result = users.getCurrentUser()) {
            is Result.Success -> {
                if (!session.applyUserUpdate(token, result.data)) AvatarUpdateResult.SessionChanged
                else if (result.data.avatar == publicUrl) AvatarUpdateResult.Saved
                else AvatarUpdateResult.Failed(AppError.Api("CLIENT_AVATAR_NOT_APPLIED"))
            }
            is Result.Error -> {
                if (result.error.isSessionTerminal) failed(result.error, token)
                else AvatarUpdateResult.NeedsVerification(publicUrl, token)
            }
        }
    }

    private fun isCurrent(token: SessionUserToken) = session.captureUserSession() == token

    private fun isUncertainWrite(error: AppError): Boolean = when (error) {
        AppError.Network, is AppError.Unknown -> true
        is AppError.Server -> error.httpCode >= 500
        is AppError.Api -> error.code in setOf("SERVER_ERROR", "SERVICE_UNAVAILABLE")
        else -> false
    }

    private fun failed(error: AppError, token: SessionUserToken): AvatarUpdateResult {
        if (!isCurrent(token)) return AvatarUpdateResult.SessionChanged
        if (error.isSessionTerminal) session.invalidateSession(token)
        return AvatarUpdateResult.Failed(error)
    }

    companion object { const val MAX_BYTES = 2 * 1024 * 1024 }
}
