package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.common.repository.HostGameSocketRepository
import android.kma.myquizzapp.core.common.repository.PlayerGameSocketRepository
import android.kma.myquizzapp.core.network.socket.HostGameSocketRepositoryImpl
import android.kma.myquizzapp.core.network.socket.PlayerGameSocketRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bind interface socket ở core:common về impl ở core:network (DIP).
 *
 * Tách riêng khỏi NetworkBindingModule vì đây là kênh realtime, không phải REST —
 * vòng đời và cách debug khác hẳn.
 *
 * KHÔNG cần provide GameSocketClient hay GameEventMapper: cả hai đều
 * constructor-injected nên Hilt tự dụng được (mapper nhận @PreserveCaseJson Json
 * do NetworkModule cung cấp).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SocketBindingModule {

    @Binds
    @Singleton
    abstract fun bindHostGameSocketRepository(
        impl: HostGameSocketRepositoryImpl
    ): HostGameSocketRepository

    @Binds
    @Singleton
    abstract fun bindPlayerGameSocketRepository(
        impl: PlayerGameSocketRepositoryImpl
    ): PlayerGameSocketRepository
}
