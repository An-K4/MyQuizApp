package android.kma.myquizzapp.feature.auth.presentation.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordResetValidationTest {
    @Test
    fun emptyPasswordIsRejected() {
        assertNotNull(AuthValidator.resetPasswordError(""))
    }

    @Test
    fun sevenCharactersAreRejected() {
        assertEquals("Mật khẩu tối thiểu 8 ký tự", AuthValidator.resetPasswordError("abcdefg"))
    }

    @Test
    fun eightCharactersDoNotRequireExtraComplexity() {
        assertNull(AuthValidator.resetPasswordError("abcdefgh"))
        assertNull(AuthValidator.resetPasswordError("12345678"))
    }

    @Test
    fun resetPreservesExistingRegisterPolicy() {
        listOf("", "short", "abcdefgh", " password ").forEach { password ->
            assertEquals(AuthValidator.registerPasswordError(password), AuthValidator.resetPasswordError(password))
        }
    }

    @Test
    fun emptyConfirmationIsRejected() {
        assertEquals("Vui lòng xác nhận mật khẩu", AuthValidator.confirmPasswordError("abcdefgh", ""))
    }

    @Test
    fun mismatchedConfirmationIsRejected() {
        assertEquals("Mật khẩu xác nhận không khớp", AuthValidator.confirmPasswordError("abcdefgh", "abcdEfgh"))
    }

    @Test
    fun matchingConfirmationIsAccepted() {
        assertNull(AuthValidator.confirmPasswordError("abcdefgh", "abcdefgh"))
    }

    @Test
    fun passwordsAreComparedWithoutTrimming() {
        assertNotNull(AuthValidator.confirmPasswordError("abcdefgh", "abcdefgh "))
        assertNull(AuthValidator.confirmPasswordError(" abcdefgh ", " abcdefgh "))
    }
}
