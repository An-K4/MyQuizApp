package com.example.myquizzapp.core.network.di

import com.example.myquizzapp.core.network.BuildConfig
import com.example.myquizzapp.core.network.result.ResultCallAdapterFactory
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase  // ← quiz_name ↔ quizName, cấu hình 1 lần cho toàn app
        ignoreUnknownKeys = true                       // backend thêm field mới → app không crash
        coerceInputValues = true                       // null rơi vào field có default → dùng default
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    // cookieJar(...) + authenticator(...) — N5 sẽ gắn vào đây

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)   // BuildConfig của core:network — không phải của app
            .client(okHttpClient)
            .addCallAdapterFactory(ResultCallAdapterFactory(json))   // unwrap → Result
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}