package android.kma.myquizzapp.core.common.validator

import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorTest {
    @Test fun commonAndUnicodeAddressesAreNotPrematurelyBlocked() {
        listOf("a@example.com", "a+b@example.com", "o'neill@example.com", "用户@example.com")
            .forEach { assertTrue(EmailValidator.validate(it) is ValidationResult.Success) }
    }

    @Test fun emptyAndObviouslyMalformedAddressesAreRejected() {
        listOf("", "  ", "example.com", "a@", "@example.com", "a@@example.com", "a b@example.com")
            .forEach { assertTrue(EmailValidator.validate(it) is ValidationResult.Error) }
    }

    @Test fun serverRemainsAuthoritativeForDetailedFormat() {
        assertTrue(EmailValidator.validate("a@b") is ValidationResult.Success)
    }
}
