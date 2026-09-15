package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.repository.GameResultRepository
import android.kma.myquizzapp.core.common.repository.StoredGameResult
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryGameResultRepository @Inject constructor() : GameResultRepository {
    private val results = ConcurrentHashMap<Long, StoredGameResult>()

    override fun save(value: StoredGameResult) {
        results[value.gameId] = value
    }

    override fun get(gameId: Long): StoredGameResult? = results[gameId]

    override fun clear(gameId: Long) {
        results.remove(gameId)
    }
}
