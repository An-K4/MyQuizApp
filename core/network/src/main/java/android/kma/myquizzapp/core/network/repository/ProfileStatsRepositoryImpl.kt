package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.GameHistoryRole
import android.kma.myquizzapp.core.common.model.MyQuizzesSort
import android.kma.myquizzapp.core.common.model.MyQuizzesVisibility
import android.kma.myquizzapp.core.common.repository.ProfileStatsRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.api.GameApiService
import android.kma.myquizzapp.core.network.api.QuizApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileStatsRepositoryImpl @Inject constructor(
    private val gameApi: GameApiService,
    private val quizApi: QuizApiService
) : ProfileStatsRepository {
    override suspend fun getParticipatedSessionCount(): Result<Int> =
        gameApi.getGameHistory(
            role = GameHistoryRole.PLAYED.apiValue,
            cursor = null,
            limit = 1,
            includeTotal = true,
            guestId = null
        ).requiredTotal()

    override suspend fun getCreatedQuizCount(): Result<Int> =
        quizApi.getMyQuizzes(
            visibility = MyQuizzesVisibility.ALL.apiValue,
            keyword = null,
            sort = MyQuizzesSort.RECENTLY_UPDATED.apiValue,
            cursor = null,
            limit = 1,
            includeTotal = true
        ).requiredTotal()

    private fun <T> Result<T>.requiredTotal(): Result<Int> = when (this) {
        is Result.Success -> page?.total?.let { Result.Success(it) }
            ?: Result.Error(AppError.Api("SERVER_ERROR"))
        is Result.Error -> this
    }
}
