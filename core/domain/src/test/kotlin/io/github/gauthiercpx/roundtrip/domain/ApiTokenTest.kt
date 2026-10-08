package io.github.gauthiercpx.roundtrip.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiTokenTest {
    @Test
    fun trimsSurroundingWhitespaceAndNewlines() {
        assertEquals("abc+/=123", normalizeApiToken("  abc+/=123\n"))
    }

    @Test
    fun blankMeansNoToken() {
        assertEquals("", normalizeApiToken("   "))
    }

    @Test
    fun rejectsInnerWhitespace() {
        assertNull(normalizeApiToken("abc def"))
    }

    @Test
    fun rejectsInnerNewlinesAndControlCharacters() {
        assertNull(normalizeApiToken("abc\ndef"))
        assertNull(normalizeApiToken("abc\u0000def"))
    }
}
