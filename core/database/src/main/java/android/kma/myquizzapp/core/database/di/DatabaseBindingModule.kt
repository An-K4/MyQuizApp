package android.kma.myquizzapp.core.database.di

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.database.cookie.RoomCookieStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// di/DatabaseBindingModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindingModule {
    @Binds
    @Singleton
    abstract fun bindCookieStore(impl: RoomCookieStore): CookieStore
}