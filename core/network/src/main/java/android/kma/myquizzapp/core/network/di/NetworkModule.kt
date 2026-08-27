package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.network.BuildConfig
import android.kma.myquizzapp.core.network.api.AuthApiService
import android.kma.myquizzapp.core.network.api.PasswordResetApiService
import android.kma.myquizzapp.core.network.api.QuizApiService
import android.kma.myquizzapp.core.network.api.StorageApiService
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
    fun provideQuizApiService(retrofit: Retrofit): QuizApiService = retrofit.create()

    /**
     * Json/Retrofit dùng chung cho các backend endpoint cần giữ nguyên tên field
     * (camelCase hoặc mixed naming), không áp dụng namingStrategy SnakeCase.
     * Các field snake_case trong mixed payload phải dùng @SerialName tường minh.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @PreserveCaseJson
    @Provides
    @Singleton
    fun providePreserveCaseJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @PreserveCaseRetrofit
    @Provides
    @Singleton
    fun providePreserveCaseRetrofit(
        @PreserveCaseJson json: Json,
        okHttpClient: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addCallAdapterFactory(ResultCallAdapterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideStorageApiService(
        @PreserveCaseRetrofit retrofit: Retrofit
    ): StorageApiService = retrofit.create()

    @Provides
    @Singleton
    fun providePasswordResetApiService(
        @PreserveCaseRetrofit retrofit: Retrofit
    ): PasswordResetApiService = retrofit.create()

    // Client riêng, KHÔNG cookie/authenticator — dùng để PUT ảnh thẳng lên storage
    // (S3-compatible, bên thứ 3). Xem RawUploadOkHttpClient để biết lý do phải tách.
    @RawUploadOkHttpClient
    @Provides
    @Singleton
    fun provideRawUploadOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
}