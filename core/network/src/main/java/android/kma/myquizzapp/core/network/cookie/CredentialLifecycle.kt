package android.kma.myquizzapp.core.network.cookie

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** Identifies a credential lifetime; it never contains a credential. */
data class CredentialEpoch(val value: Long)
internal data class RefreshVersion(val value: Long)

/**
 * Serializes the epoch check and local cookie access. The lock is never held across
 * network I/O. All accesses that can mutate credentials must use this coordinator.
 */
@Singleton
class CredentialLifecycle @Inject constructor(private val store: CookieStore) {
    private val lock = Any()
    private var epoch = 0L
    private var revision = 0L
    private var authenticationPending = false
    // If persistent deletion fails, never reuse possibly stale cookies in this process.
    private var storageBlocked = false
    private var refreshVersion = 0L

    fun current(): CredentialEpoch = synchronized(lock) { CredentialEpoch(epoch) }

    fun <T> snapshot(action: (CredentialEpoch) -> T): T = synchronized(lock) {
        action(CredentialEpoch(epoch))
    }

    fun beginNewLifetime(action: () -> Unit = {}): CredentialEpoch = synchronized(lock) {
        epoch++
        revision++
        authenticationPending = false
        action()
        CredentialEpoch(epoch)
    }

    fun beginFreshAuthentication(action: () -> Unit): CredentialEpoch = synchronized(lock) {
        epoch++
        revision++
        authenticationPending = true
        action()
        clearStorage()
        CredentialEpoch(epoch)
    }

    fun finishAuthentication(expected: CredentialEpoch): Boolean = synchronized(lock) {
        if (expected.value != epoch || !authenticationPending) return@synchronized false
        authenticationPending = false
        true
    }

    fun abortAuthentication(expected: CredentialEpoch, action: () -> Unit): Boolean = synchronized(lock) {
        if (expected.value != epoch || !authenticationPending) return@synchronized false
        authenticationPending = false
        epoch++
        revision++
        action()
        clearStorage()
        true
    }

    fun isCurrent(expected: CredentialEpoch): Boolean = synchronized(lock) {
        expected.value == epoch
    }

    internal fun refreshVersion(expected: CredentialEpoch): RefreshVersion? = synchronized(lock) {
        if (expected.value == epoch) RefreshVersion(refreshVersion) else null
    }

    internal fun markRefreshed(expected: CredentialEpoch): Boolean = synchronized(lock) {
        if (expected.value != epoch) return@synchronized false
        refreshVersion++
        true
    }

    fun revision(expected: CredentialEpoch): Long? = synchronized(lock) {
        if (expected.value == epoch) revision else null
    }

    fun load(expected: CredentialEpoch, host: String, allowPending: Boolean = true): List<StoredCookie>? = synchronized(lock) {
        if (storageBlocked || expected.value != epoch || (authenticationPending && !allowPending)) return@synchronized null
        runBlocking { store.loadForHost(host) }
    }

    fun save(expected: CredentialEpoch, host: String, cookies: List<StoredCookie>): Boolean =
        synchronized(lock) {
            if (storageBlocked || expected.value != epoch) return@synchronized false
            if (cookies.isNotEmpty()) {
                runBlocking { store.saveAll(host, cookies) }
                revision++
            }
            true
        }

    fun retireAndClear(expected: CredentialEpoch): Boolean = synchronized(lock) {
        if (expected.value != epoch) return@synchronized false
        // Retire even if the storage operation fails; stale responses must stay retired.
        authenticationPending = false
        epoch++
        revision++
        clearStorage()
        true
    }

    private fun clearStorage() {
        storageBlocked = true
        runBlocking { store.clear() }
        storageBlocked = false
    }

    fun <T> ifCurrent(expected: CredentialEpoch, action: () -> T): T? = synchronized(lock) {
        if (expected.value != epoch) null else action()
    }
}
