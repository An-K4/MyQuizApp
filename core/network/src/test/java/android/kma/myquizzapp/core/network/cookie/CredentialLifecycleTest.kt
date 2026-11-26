package android.kma.myquizzapp.core.network.cookie

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CredentialLifecycleTest {
    private val store = LifecycleTestStore()
    private val lifecycle = CredentialLifecycle(store)
    private val host = "api.example.test"

    @Test fun `retired response cannot write or clear newer login`() {
        val old = lifecycle.current()
        val fresh = lifecycle.beginNewLifetime()
        assertTrue(lifecycle.save(fresh, host, listOf(cookie("new-fixture"))))
        assertFalse(lifecycle.save(old, host, listOf(cookie("old-fixture"))))
        assertFalse(lifecycle.retireAndClear(old))
        assertEquals("new-fixture", lifecycle.load(fresh, host)?.single()?.value)
    }

    @Test fun `retirement clears and rejects late response`() {
        val old = lifecycle.current()
        assertTrue(lifecycle.save(old, host, listOf(cookie("old-fixture"))))
        assertTrue(lifecycle.retireAndClear(old))
        assertFalse(lifecycle.save(old, host, listOf(cookie("late-fixture"))))
        assertNull(lifecycle.load(old, host))
        assertTrue(lifecycle.load(lifecycle.current(), host).orEmpty().isEmpty())
    }

    @Test fun `current response advances revision while stale one does not`() {
        val old = lifecycle.current()
        val initial = lifecycle.revision(old)!!
        assertTrue(lifecycle.save(old, host, listOf(cookie("one-fixture"))))
        assertTrue(lifecycle.revision(old)!! > initial)
        val fresh = lifecycle.beginNewLifetime()
        val after = lifecycle.revision(fresh)
        assertFalse(lifecycle.save(old, host, listOf(cookie("late-fixture"))))
        assertEquals(after, lifecycle.revision(fresh))
    }

    @Test fun `failed deletion blocks use of stored cookies until a successful clear`() {
        var failClear = true
        val failingStore = object : CookieStore {
            override suspend fun loadForHost(host: String) = listOf(cookie("old-fixture"))
            override suspend fun saveAll(host: String, cookies: List<StoredCookie>) = Unit
            override suspend fun clear() { if (failClear) throw IllegalStateException("storage unavailable") }
        }
        val guarded = CredentialLifecycle(failingStore)
        val old = guarded.current()
        try {
            guarded.retireAndClear(old)
            throw AssertionError("Expected failed clear")
        } catch (_: IllegalStateException) { }
        val current = guarded.current()
        assertNull(guarded.load(current, host))
        assertFalse(guarded.save(current, host, listOf(cookie("late-fixture"))))
        failClear = false
        assertTrue(guarded.retireAndClear(current))
        assertTrue(guarded.save(guarded.current(), host, emptyList()))
    }

    @Test fun `epoch change cannot interleave with guarded cookie write`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val changeStarted = CountDownLatch(1)
        val guardedStore = object : CookieStore {
            override suspend fun loadForHost(host: String) = emptyList<StoredCookie>()
            override suspend fun saveAll(host: String, cookies: List<StoredCookie>) {
                started.countDown()
                check(release.await(5, TimeUnit.SECONDS))
            }
            override suspend fun clear() = Unit
        }
        val guarded = CredentialLifecycle(guardedStore)
        val old = guarded.current()
        val workers = Executors.newFixedThreadPool(2)
        try {
            val save = workers.submit<Boolean> { guarded.save(old, host, listOf(cookie("fixture"))) }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val change = workers.submit<CredentialEpoch> {
                changeStarted.countDown()
                guarded.beginNewLifetime()
            }
            assertTrue(changeStarted.await(5, TimeUnit.SECONDS))
            assertFalse(change.isDone)
            release.countDown()
            assertTrue(save.get(10, TimeUnit.SECONDS))
            val fresh = change.get(10, TimeUnit.SECONDS)
            assertFalse(guarded.save(old, host, emptyList()))
            assertTrue(guarded.isCurrent(fresh))
        } finally {
            release.countDown()
            workers.shutdownNow()
        }
    }

    private fun cookie(value: String) =
        StoredCookie("session", value, host, "/", Long.MAX_VALUE, false, true)
}

private class LifecycleTestStore : CookieStore {
    private var values = emptyList<StoredCookie>()
    override suspend fun loadForHost(host: String) = values.filter { it.domain == host }
    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) { values = cookies }
    override suspend fun clear() { values = emptyList() }
}
