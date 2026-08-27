package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.network.dto.CompleteResetRequest
import android.kma.myquizzapp.core.network.dto.PresignResponseDto
import android.kma.myquizzapp.core.network.dto.PresignUploadRequestDto
import android.kma.myquizzapp.core.network.dto.ResetTicketDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreserveCaseJsonTest {

    private val json = NetworkModule.providePreserveCaseJson()

    @Test
    fun `storage request keeps camelCase field names`() {
        val encoded = json.encodeToString(
            PresignUploadRequestDto(
                contentType = "image/jpeg",
                folder = "quizzes",
                fileSize = 1024L
            )
        )

        assertTrue(encoded.contains("\"contentType\""))
        assertTrue(encoded.contains("\"fileSize\""))
        assertFalse(encoded.contains("content_type"))
        assertFalse(encoded.contains("file_size"))
    }

    @Test
    fun `storage response reads camelCase field names`() {
        val decoded = json.decodeFromString<PresignResponseDto>(
            """{"presignedUrl":{"uploadUrl":"https://upload.test","publicUrl":"https://public.test","key":"quizzes/1/test.jpg"}}"""
        )

        assertEquals("https://upload.test", decoded.presignedUrl.uploadUrl)
        assertEquals("https://public.test", decoded.presignedUrl.publicUrl)
        assertEquals("quizzes/1/test.jpg", decoded.presignedUrl.key)
    }

    @Test
    fun `password reset request keeps newPassword camelCase`() {
        val encoded = json.encodeToString(
            CompleteResetRequest(
                ticket = "reset-ticket",
                newPassword = "Password123"
            )
        )

        assertTrue(encoded.contains("\"newPassword\""))
        assertFalse(encoded.contains("new_password"))
    }

    @Test
    fun `password reset response reads camelCase field names`() {
        val decoded = json.decodeFromString<ResetTicketDto>(
            """{"ticket":"reset-ticket","expiresAt":"2026-08-27T15:00:00.000Z","email":"k***@example.com"}"""
        )

        assertEquals("reset-ticket", decoded.ticket)
        assertEquals("2026-08-27T15:00:00.000Z", decoded.expiresAt)
        assertEquals("k***@example.com", decoded.email)
    }

    @Test
    fun `mixed payload keeps explicit snake case and nested camelCase`() {
        val encoded = json.encodeToString(
            MixedRequest(
                quizId = 42L,
                config = MixedConfig(totalMatchSeconds = 300)
            )
        )

        assertTrue(encoded.contains("\"quiz_id\""))
        assertTrue(encoded.contains("\"totalMatchSeconds\""))
        assertFalse(encoded.contains("quizId"))
        assertFalse(encoded.contains("total_match_seconds"))
    }

    @Serializable
    private data class MixedRequest(
        @SerialName("quiz_id") val quizId: Long,
        val config: MixedConfig
    )

    @Serializable
    private data class MixedConfig(
        val totalMatchSeconds: Int
    )
}
