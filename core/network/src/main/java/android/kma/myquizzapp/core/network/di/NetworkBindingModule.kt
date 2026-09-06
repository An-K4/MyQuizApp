package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.repository.StorageRepository
import android.kma.myquizzapp.core.network.repository.AuthRepositoryImpl
import android.kma.myquizzapp.core.network.repository.GameSessionRepositoryImpl
import android.kma.myquizzapp.core.network.repository.QuizRepositoryImpl
import android.kma.myquizzapp.core.network.repository.SessionRepositoryImpl
import android.kma.myquizzapp.core.network.repository.StorageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindQuizRepository(quizRepositoryImpl: QuizRepositoryImpl): QuizRepository

    @Binds
    @Singleton
    abstract fun bindStorageRepository(storageRepositoryImpl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindGameSessionRepository(
        gameSessionRepositoryImpl: GameSessionRepositoryImpl
    ): GameSessionRepository

    // N19.6: @Singleton ở đây là bắt buộc, không phải tối ưu. SessionRepository
    // GIỮ STATE (StateFlow trạng thái đăng nhập); nhiều instance = nhiều nguồn
    // sự thật, đúng cái bệnh N19.6 đang sửa.
    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        sessionRepositoryImpl: SessionRepositoryImpl
    ): SessionRepository
}