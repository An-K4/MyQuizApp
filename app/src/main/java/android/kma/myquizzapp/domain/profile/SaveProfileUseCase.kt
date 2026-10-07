package android.kma.myquizzapp.domain.profile

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.isSessionTerminal
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.repository.UserRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

class SaveProfileUseCase @Inject constructor(
    private val users: UserRepository,
    private val session: SessionRepository
) {
    suspend operator fun invoke(draft: ProfileDraft, token: SessionUserToken): Result<Unit> {
        if (session.captureUserSession() != token) return Result.Error(AppError.Unauthorized)
        if (draft.errors().hasErrors) return Result.Error(AppError.Api("VALIDATION_ERROR"))
        val patch = draft.patch()
        if (patch.isEmpty) return Result.Success(Unit)
        return when (val result = users.updateProfile(patch)) {
            is Result.Success -> if (session.applyUserUpdate(token, result.data)) Result.Success(Unit)
                else Result.Error(AppError.Unauthorized)
            is Result.Error -> {
                if (result.error.isSessionTerminal) session.invalidateSession(token)
                result
            }
        }
    }
}
