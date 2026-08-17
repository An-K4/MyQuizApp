package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.network.repository.AuthRepositoryImpl
import android.kma.myquizzapp.core.network.repository.QuizRepositoryImpl
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
}