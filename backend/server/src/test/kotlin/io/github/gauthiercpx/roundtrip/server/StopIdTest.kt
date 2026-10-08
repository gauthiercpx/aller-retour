package io.github.gauthiercpx.roundtrip.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StopIdTest {
    @Test
    fun parsesPrefixedIdsInRequestOrder() {
        val stops = parseStopIds("star-metro:5074,idfm:58572,star-bus:1317")

        assertEquals(
            listOf(
                StopId(StopSource.STAR_METRO, "5074"),
                StopId(StopSource.IDFM, "58572"),
                StopId(StopSource.STAR_BUS, "1317"),
            ),
            stops,
        )
    }

    @Test
    fun trimsWhitespaceAroundIds() {
        val stops = parseStopIds(" idfm:58572 , star-metro:5074 ")

        assertEquals(listOf(StopId(StopSource.IDFM, "58572"), StopId(StopSource.STAR_METRO, "5074")), stops)
    }

    @Test
    fun removesDuplicatesKeepingFirstOccurrence() {
        val stops = parseStopIds("idfm:58572,star-metro:5074,idfm:58572")

        assertEquals(listOf(StopId(StopSource.IDFM, "58572"), StopId(StopSource.STAR_METRO, "5074")), stops)
    }

    @Test
    fun rejectsMissingParameter() {
        assertFailsWith<InvalidStopsException> { parseStopIds(null) }
    }

    @Test
    fun rejectsBlankParameter() {
        assertFailsWith<InvalidStopsException> { parseStopIds("  ") }
    }

    @Test
    fun rejectsUnknownPrefix() {
        assertFailsWith<InvalidStopsException> { parseStopIds("sncf:87271007") }
    }

    @Test
    fun rejectsIdWithoutPrefix() {
        assertFailsWith<InvalidStopsException> { parseStopIds("58572") }
    }

    @Test
    fun rejectsRawUpstreamReference() {
        assertFailsWith<InvalidStopsException> { parseStopIds("idfm:STIF:StopArea:SP:58572:") }
    }

    @Test
    fun rejectsQueryInjectionAttempt() {
        assertFailsWith<InvalidStopsException> { parseStopIds("star-metro:5074\" or idarret like \"%") }
    }

    @Test
    fun rejectsEmptyEntryBetweenCommas() {
        assertFailsWith<InvalidStopsException> { parseStopIds("idfm:58572,,star-metro:5074") }
    }

    @Test
    fun rejectsIdLongerThanNineDigits() {
        assertFailsWith<InvalidStopsException> { parseStopIds("idfm:1234567890") }
    }

    @Test
    fun acceptsExactlyTenStops() {
        val stops = parseStopIds("idfm:1,idfm:2,idfm:3,idfm:4,idfm:5,idfm:6,idfm:7,idfm:8,idfm:9,idfm:10")

        assertEquals(10, stops.size)
    }

    @Test
    fun rejectsMoreThanTenDistinctStops() {
        assertFailsWith<InvalidStopsException> {
            parseStopIds("idfm:1,idfm:2,idfm:3,idfm:4,idfm:5,idfm:6,idfm:7,idfm:8,idfm:9,idfm:10,idfm:11")
        }
    }
}
