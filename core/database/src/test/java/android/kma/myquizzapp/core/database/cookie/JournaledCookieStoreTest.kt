package android.kma.myquizzapp.core.database.cookie

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class JournaledCookieStoreTest {
    private val host = "api.example.test"
    private val cookie = StoredCookie("sid", "fixture", host, "/", Long.MAX_VALUE, true, true)

    @Test fun failedClearIsRetriedBeforeReadAfterRecreation() = runBlocking {
        val journal = FakeJournal()
        val underlying = FakeStore(mutableListOf(cookie))
        underlying.failClear = true
        try { JournaledCookieStore(underlying, journal).clear(); fail("clear must fail") } catch (_: IOException) { }
        assertTrue(journal.pendingFlag)
        val restarted = JournaledCookieStore(underlying, journal)
        try { restarted.loadForHost(host); fail("old credentials must not be returned") } catch (_: IOException) { }
        assertTrue(journal.pendingFlag)
        underlying.failClear = false
        assertTrue(restarted.loadForHost(host).isEmpty())
        assertFalse(journal.pendingFlag)
    }

    @Test fun pendingCleanupMustCompleteBeforeSavingNewCookies() = runBlocking {
        val journal = FakeJournal().apply { pendingFlag = true }
        val underlying = FakeStore(mutableListOf(cookie)).apply { failClear = true }
        val restarted = JournaledCookieStore(underlying, journal)
        try { restarted.saveAll(host, listOf(cookie.copy(value = "new"))); fail("must not save") } catch (_: IOException) { }
        assertEquals("fixture", underlying.cookies.single().value)
        underlying.failClear = false
        restarted.saveAll(host, listOf(cookie.copy(value = "new")))
        assertEquals("new", restarted.loadForHost(host).single().value)
        assertFalse(journal.pendingFlag)
    }

    @Test fun failedJournalWriteStillAttemptsDeletionAndReportsFailure() = runBlocking {
        val journal = FakeJournal().apply { failWrite = true }
        val underlying = FakeStore(mutableListOf(cookie))
        try { JournaledCookieStore(underlying, journal).clear(); fail("must fail") } catch (_: IOException) { }
        assertEquals(1, underlying.clears)
        assertTrue(JournaledCookieStore(underlying, FakeJournal()).loadForHost(host).isEmpty())
    }

    @Test fun failedJournalAndDeletionReportsBothFailures() = runBlocking {
        val journal = FakeJournal().apply { failWrite = true }
        val underlying = FakeStore(mutableListOf(cookie)).apply { failClear = true }
        try {
            JournaledCookieStore(underlying, journal).clear()
            fail("must fail")
        } catch (e: IOException) {
            assertEquals(1, e.suppressed.size)
        }
        assertEquals(1, underlying.clears)
        // No durable marker or deletion: recovery after restart is not guaranteed.
        assertEquals("fixture", underlying.cookies.single().value)
    }

    @Test fun missingMarkerPreservesExistingUpgradeSession() = runBlocking {
        val underlying = FakeStore(mutableListOf(cookie))
        assertEquals("fixture", JournaledCookieStore(underlying, FakeJournal()).loadForHost(host).single().value)
        assertEquals(0, underlying.clears)
    }
}

private class FakeJournal : ClearJournal {
    var pendingFlag = false
    var failWrite = false
    override fun isPending() = pendingFlag
    override fun setPending(value: Boolean) {
        if (failWrite) throw IOException("fixture failure")
        pendingFlag = value
    }
}

private class FakeStore(val cookies: MutableList<StoredCookie>) : CookieStore {
    var failClear = false
    var clears = 0
    override suspend fun loadForHost(host: String) = cookies.toList()
    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) { this.cookies.addAll(cookies) }
    override suspend fun clear() {
        clears++
        if (failClear) throw IOException("fixture failure")
        cookies.clear()
    }
}
