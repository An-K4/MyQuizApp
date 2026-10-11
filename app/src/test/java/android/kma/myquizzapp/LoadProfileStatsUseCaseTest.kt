package android.kma.myquizzapp

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.repository.ProfileStatsRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.domain.profile.LoadProfileStatsUseCase
import android.kma.myquizzapp.domain.profile.ProfileStats
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadProfileStatsUseCaseTest {
    @Test fun `combines exact server totals`() = runTest {
        val useCase = LoadProfileStatsUseCase(FakeProfileStatsRepository(
            sessions = Result.Success(17), quizzes = Result.Success(4)
        ))

        assertEquals(
            Result.Success(ProfileStats(participatedSessionCount = 17, createdQuizCount = 4)),
            useCase()
        )
    }

    @Test fun `does not provide partial totals when one request fails`() = runTest {
        val error = Result.Error(AppError.Network)
        val useCase = LoadProfileStatsUseCase(FakeProfileStatsRepository(
            sessions = Result.Success(17), quizzes = error
        ))

        assertTrue(useCase() is Result.Error)
    }

    private class FakeProfileStatsRepository(
        private val sessions: Result<Int>,
        private val quizzes: Result<Int>
    ) : ProfileStatsRepository {
        override suspend fun getParticipatedSessionCount(): Result<Int> = sessions
        override suspend fun getCreatedQuizCount(): Result<Int> = quizzes
    }
}
