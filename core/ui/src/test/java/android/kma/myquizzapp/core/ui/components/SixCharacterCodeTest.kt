package android.kma.myquizzapp.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SixCharacterCodeTest {
    @Test fun emptyInputStaysEmpty() {
        assertEquals("", normalizeSixCharacterCode("", SixCharacterCodeKind.Otp))
    }
    @Test fun otpKeepsAsciiDigits() {
        assertEquals("482123", normalizeSixCharacterCode("482123", SixCharacterCodeKind.Otp))
    }
    @Test fun otpDropsLettersAndWhitespace() {
        assertEquals("123456", normalizeSixCharacterCode("a1 2\n3-4b5.6", SixCharacterCodeKind.Otp))
    }
    @Test fun otpRejectsNonAsciiDigits() {
        assertEquals("12", normalizeSixCharacterCode("١٢１２12", SixCharacterCodeKind.Otp))
    }
    @Test fun otpPasteIsLimitedToSixDigits() {
        assertEquals("123456", normalizeSixCharacterCode("123456789", SixCharacterCodeKind.Otp))
    }
    @Test fun roomKeepsLettersAndNumbers() {
        assertEquals("AB23CD", normalizeSixCharacterCode("AB23CD", SixCharacterCodeKind.Room))
    }
    @Test fun roomUppercasesAsciiLetters() {
        assertEquals("AB23CD", normalizeSixCharacterCode("ab23cd", SixCharacterCodeKind.Room))
    }
    @Test fun roomDropsSeparatorsBeforeTruncation() {
        assertEquals("AB23CD", normalizeSixCharacterCode("a b-2\n3_c dEXTRA", SixCharacterCodeKind.Room))
    }
    @Test fun roomDoesNotExpandUnicodeIntoAscii() {
        assertEquals("AB12", normalizeSixCharacterCode("áßđAB12", SixCharacterCodeKind.Room))
    }
    @Test fun deletingCharactersDoesNotPadOrSubmit() {
        assertEquals("123", normalizeSixCharacterCode("123", SixCharacterCodeKind.Otp))
        assertEquals("", normalizeSixCharacterCode("!", SixCharacterCodeKind.Room))
    }
}
