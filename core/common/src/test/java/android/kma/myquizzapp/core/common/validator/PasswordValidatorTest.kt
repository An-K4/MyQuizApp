package android.kma.myquizzapp.core.common.validator

import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidatorTest {
    @Test fun loginRequiresNonemptyPasswordWithoutTrimming() {
        assertTrue(PasswordValidator.validate("") is ValidationResult.Error)
        assertTrue(PasswordValidator.validate(" ") is ValidationResult.Success)
        assertTrue(PasswordValidator.validate(" a ") is ValidationResult.Success)
    }

    @Test fun registerUsesUtf16LengthWithoutTrimming() {
        assertTrue(PasswordValidator.validateForRegister("       ") is ValidationResult.Error)
        assertTrue(PasswordValidator.validateForRegister("        ") is ValidationResult.Success)
        assertTrue(PasswordValidator.validateForRegister("😀😀😀😀") is ValidationResult.Success)
    }

    @Test fun confirmationRequiresExactUnchangedValue() {
        assertTrue(PasswordValidator.validateConfirm(" ", "") is ValidationResult.Error)
        assertTrue(PasswordValidator.validateConfirm("        ", "        ") is ValidationResult.Success)
        assertTrue(PasswordValidator.validateConfirm(" password ", "password") is ValidationResult.Error)
    }
}
