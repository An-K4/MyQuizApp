package com.example.myquizzapp.core.database.di

import com.example.myquizzapp.core.common.cookie.CookieStore
import com.example.myquizzapp.core.database.cookie.RoomCookieStore
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