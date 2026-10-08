package io.github.gauthiercpx.roundtrip.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StopListTest {
    @Test
    fun blankInputGivesAnEmptyValidList() {
        assertEquals(StopListParse.Valid(emptyList()), parseStopList("  "))
    }

    @Test
    fun trimsDropsBlanksAndDuplicatesKeepingOrder() {
        val result = parseStopList(" idfm:58572, star-metro:5074 ,,idfm:58572")

        assertEquals(StopListParse.Valid(listOf("idfm:58572", "star-metro:5074")), result)
    }

    @Test
    fun rejectsUnknownPrefix() {
        val result = assertIs<StopListParse.Invalid>(parseStopList("sncf:123"))

        assertTrue("sncf:123" in result.message)
    }

    @Test
    fun rejectsNonDigitLocalId() {
        assertIs<StopListParse.Invalid>(parseStopList("idfm:12a"))
    }

    @Test
    fun rejectsLocalIdLongerThanNineDigits() {
        assertIs<StopListParse.Invalid>(parseStopList("idfm:1234567890"))
    }

    @Test
    fun acceptsExactlyTheMaximumNumberOfStops() {
        val raw = (1..MAX_STOPS_PER_REQUEST).joinToString(",") { "idfm:$it" }

        assertIs<StopListParse.Valid>(parseStopList(raw))
    }

    @Test
    fun rejectsMoreThanTheMaximumNumberOfStops() {
        val raw = (1..MAX_STOPS_PER_REQUEST + 1).joinToString(",") { "idfm:$it" }

        assertIs<StopListParse.Invalid>(parseStopList(raw))
    }
}
