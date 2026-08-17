package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.model.QuizCard
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.result.map
import android.kma.myquizzapp.core.network.api.QuizApiService
import android.kma.myquizzapp.core.network.dto.toDomain
import javax.inject.Inject

/**
 * Implementation của QuizRepository interface.
 * 
 * Dùng QuizApiService (Retrofit) để gọi backend API và map DTO sang domain models.
 */
class QuizRepositoryImpl @Inject constructor(
    private val quizApi: QuizApiService
) : QuizRepository {
    
    override suspend fun getHomeContent(): Result<List<HomeSection>> =
        quizApi.getHomeContent().map { it.toDomain() }
    
    override suspend fun searchQuizzes(keyword: String): Result<List<QuizCard>> =
        quizApi.searchQuizzes(keyword).map { dtoList -> 
            dtoList.map { it.toDomain() }
        }
    
    override suspend fun getMyQuizzes(): Result<List<QuizCard>> =
        quizApi.getMyQuizzes().map { dtoList -> 
            dtoList.map { it.toDomain() }
        }
    
    override suspend fun getQuizDetail(quizId: Long): Result<Quiz> =
        quizApi.getQuizDetail(quizId)
}
