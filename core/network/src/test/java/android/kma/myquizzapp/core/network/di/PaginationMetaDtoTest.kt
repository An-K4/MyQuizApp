package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.network.dto.PaginationMetaDto
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaginationMetaDtoTest {
    @Test
    fun `production snake case json still reads camel case pagination meta`() {
        val json = NetworkModule.provideJson()
        val meta = json.decodeFromString<PaginationMetaDto>(
            """{"limit":12,"nextCursor":"cursor-2","hasMore":true,"total":198}"""
        )

        assertEquals(12, meta.limit)
        assertEquals("cursor-2", meta.nextCursor)
        assertTrue(meta.hasMore)
        assertEquals(198, meta.total)
    }
}
