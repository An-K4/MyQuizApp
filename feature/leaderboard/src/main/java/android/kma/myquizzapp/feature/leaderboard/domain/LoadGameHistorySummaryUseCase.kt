package android.kma.myquizzapp.feature.leaderboard.domain

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.GameHistorySummary
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.datastore.GuestIdentityStore
import javax.inject.Inject

/** Tải tổng quan một trận cũ bằng cookie user hoặc UUID guest đã tồn tại. */
class LoadGameHistorySummaryUseCase @Inject constructor(
    private val repository: GameSessionRepository,
    private val guestIdentityStore: GuestIdentityStore
) {
    suspend operator fun invoke(gameId: Long, session: SessionState): Result<GameHistorySummary> {
        val guestId = when (session) {
            is SessionState.LoggedIn -> null
            SessionState.Guest -> guestIdentityStore.getGuestIdOrNull()
                ?: return Result.Error(AppError.Unknown(IllegalStateException("Guest identity is missing")))
            SessionState.Unknown -> return Result.Error(
                AppError.Unknown(IllegalStateException("Session is not resolved"))
            )
        }
        return repository.getGameHistorySummary(gameId, guestId)
    }
}
