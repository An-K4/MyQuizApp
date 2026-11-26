package android.kma.myquizzapp.core.database.cookie

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A durable, non-secret intent to delete all credentials. */
internal interface ClearJournal {
    fun isPending(): Boolean
    fun setPending(value: Boolean)
}

/** Never expose or write cookies while a previously recorded clear is incomplete. */
internal class JournaledCookieStore(
    private val delegate: CookieStore,
    private val journal: ClearJournal
) : CookieStore {
    private val lock = Mutex()

    private suspend fun recoverIfPending() {
        if (journal.isPending()) {
            delegate.clear()
            journal.setPending(false)
        }
    }

    override suspend fun loadForHost(host: String): List<StoredCookie> = lock.withLock {
        recoverIfPending()
        delegate.loadForHost(host)
    }

    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) = lock.withLock {
        recoverIfPending()
        delegate.saveAll(host, cookies)
    }

    override suspend fun clear() = lock.withLock {
        // Normally persist intent first. If the journal fails, still try deleting
        // the cookies, but surface the error because durable state is uncertain.
        try {
            journal.setPending(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            try {
                delegate.clear()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (clearFailure: Exception) {
                e.addSuppressed(clearFailure)
            }
            throw e
        }
        delegate.clear()
        journal.setPending(false)
    }
}
