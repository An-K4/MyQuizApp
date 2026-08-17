package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.network.BuildConfig
import android.kma.myquizzapp.core.network.api.AuthApiService
import android.kma.myquizzapp.core.network.api.UserApiService
import android.kma.myquizzapp.core.network.cookie.PersistentCookieJar
import android.kma.myquizzapp.core.network.cookie.TokenAuthenticator
import android.kma.myquizzapp.core.network.repository.AuthRepositoryImpl
import android.kma.myquizzapp.core.network.result.ResultCallAdapterFactory
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.CookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
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
    fun provideOkHttpClient(
        cookieJar: CookieJar,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(cookieJar)                    // ← thêm
            .authenticator(tokenAuthenticator)       // ← thêm
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)   // BuildConfig của core:network — không phải của app
            .client(okHttpClient)
            .addCallAdapterFactory(ResultCallAdapterFactory(json))   // unwrap → Result
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideCookieJar(store: CookieStore): CookieJar = PersistentCookieJar(store)

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService = retrofit.create()

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService = retrofit.create()
    
    @Provides
    @Singleton
    fun provideQuizApiService(retrofit: Retrofit): android.kma.myquizzapp.core.network.api.QuizApiService = retrofit.create()
}