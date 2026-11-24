package android.kma.myquizzapp.feature.auth.presentation.validation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RegisterInputValidationTest {
    @Test fun emailIsTrimmedBeforeValidation() {
        assertNull(AuthValidator.emailError("  user@example.com  "))
        assertNotNull(AuthValidator.emailError(" user @example.com "))
        assertNotNull(AuthValidator.emailError("  "))
    }

    @Test fun fullnameIsCheckedAfterTheSameTrimAsTheRequest() {
        assertNull(AuthValidator.fullnameError("  " + "a".repeat(100) + "  "))
        assertNotNull(AuthValidator.fullnameError("  " + "a".repeat(101) + "  "))
        assertNotNull(AuthValidator.fullnameError(" a "))
        assertNull(AuthValidator.fullnameError(" 😀 ")) // two UTF-16 code units
    }

    @Test fun optionalPhoneIsCheckedAfterTheSameTrimAsTheRequest() {
        assertNull(AuthValidator.phoneError("  "))
        assertNull(AuthValidator.phoneError(" +1234567 "))
        assertNull(AuthValidator.phoneError("123456789012345"))
        assertNotNull(AuthValidator.phoneError("123456"))
        assertNotNull(AuthValidator.phoneError("1234567890123456"))
        assertNotNull(AuthValidator.phoneError("123 4567"))
    }
}
