package android.kma.myquizzapp.domain.security

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.AuthProvider
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.repository.UserRepository
import android.kma.myquizzapp.core.common.result.Result
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(private val users: UserRepository, private val session: SessionRepository) {
    suspend operator fun invoke(old: String, new: String, confirm: String, token: SessionUserToken): SecurityMutationResult {
        if (session.captureUserSession() != token) return SecurityMutationResult.SessionChanged
        if (session.state.value.userOrNull?.authProvider == AuthProvider.GOOGLE)
            return SecurityMutationResult.Rejected(AppError.Api("AUTH_GOOGLE_ONLY"))
        if (SecurityValidation.change(old, new, confirm).hasErrors)
            return SecurityMutationResult.Rejected(AppError.Api("VALIDATION_ERROR"))
        val response = users.changePassword(old, new)
        if (session.captureUserSession() != token) return SecurityMutationResult.SessionChanged
        return when (response) {
            is Result.Success -> SecurityMutationResult.Completed // Server does NOT revoke sessions.
            is Result.Error -> when {
                response.error.securitySessionTerminal -> retire(token)
                response.error.mutationUncertain -> SecurityMutationResult.Uncertain
                else -> SecurityMutationResult.Rejected(response.error)
            }
        }
    }

    private suspend fun retire(token: SessionUserToken): SecurityMutationResult = try {
        if (session.clearSession(token)) SecurityMutationResult.SessionEnded else SecurityMutationResult.SessionChanged
    } catch (e: CancellationException) { throw e }
      catch (_: Exception) { SecurityMutationResult.CleanupPending }
}
