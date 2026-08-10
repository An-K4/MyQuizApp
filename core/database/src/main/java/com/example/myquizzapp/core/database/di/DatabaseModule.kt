package com.example.myquizzapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.myquizzapp.core.database.MyQuizzDatabase
import com.example.myquizzapp.core.database.dao.CookieDao
import com.example.myquizzapp.core.database.dao.GameHistoryDao
import com.example.myquizzapp.core.database.dao.QuizCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MyQuizzDatabase =
        Room.databaseBuilder(context, MyQuizzDatabase::class.java, "myquizz.db").build()

    @Provides fun provideCookieDao(db: MyQuizzDatabase): CookieDao = db.cookieDao()
    @Provides fun provideQuizCacheDao(db: MyQuizzDatabase): QuizCacheDao = db.quizCacheDao()
    @Provides fun provideGameHistoryDao(db: MyQuizzDatabase): GameHistoryDao = db.gameHistoryDao()
}