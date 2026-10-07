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

class DeactivateAccountUseCase @Inject constructor(private val users: UserRepository, private val session: SessionRepository) {
    suspend operator fun invoke(password: String, confirmed: Boolean, token: SessionUserToken): SecurityMutationResult {
        if (session.captureUserSession() != token) return SecurityMutationResult.SessionChanged
        if (session.state.value.userOrNull?.authProvider == AuthProvider.GOOGLE)
            return SecurityMutationResult.Rejected(AppError.Api("AUTH_GOOGLE_ONLY"))
        if (!confirmed || SecurityValidation.deactivate(password) != null)
            return SecurityMutationResult.Rejected(AppError.Api("VALIDATION_ERROR"))
        val response = users.deactivateAccount(password)
        if (session.captureUserSession() != token) return SecurityMutationResult.SessionChanged
        return when (response) {
            is Result.Success -> finishLocal(token, completed = true)
            is Result.Error -> when {
                response.error.securitySessionTerminal -> finishLocal(token, completed = false)
                response.error.mutationUncertain -> SecurityMutationResult.Uncertain
                else -> SecurityMutationResult.Rejected(response.error)
            }
        }
    }

    /** Retry only local cleanup; never send DELETE twice after a confirmed mutation. */
    suspend fun finishLocal(token: SessionUserToken, completed: Boolean = false): SecurityMutationResult = try {
        if (session.clearSession(token)) {
            if (completed) SecurityMutationResult.Completed else SecurityMutationResult.SessionEnded
        } else SecurityMutationResult.SessionChanged
    } catch (e: CancellationException) { throw e }
      catch (_: Exception) { SecurityMutationResult.CleanupPending }
}
