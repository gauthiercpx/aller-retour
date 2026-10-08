package io.github.gauthiercpx.roundtrip.widget

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.model.Vehicle
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WidgetFormatTest {
    private val paris = ZoneId.of("Europe/Paris")

    private fun departure(
        expected: String = "2026-10-08T12:32:00Z",
        delaySeconds: Int? = null,
        status: DepartureStatus = DepartureStatus.ON_TIME,
        isRealtime: Boolean = true,
    ) = Departure(
        network = Network.IDFM,
        stopId = "idfm:1",
        stopName = "Stop",
        lineId = "L",
        lineName = "E",
        lineColor = null,
        mode = Mode.RER,
        direction = null,
        destination = "Tournan",
        scheduledTime = null,
        expectedTime = Instant.parse(expected),
        delaySeconds = delaySeconds,
        status = status,
        isRealtime = isRealtime,
        vehicle = Vehicle.UNKNOWN,
        missionCode = null,
    )

    @Test
    fun formatsAbsoluteClockTimeInTheGivenZone() {
        assertEquals("14:32", formatClock(Instant.parse("2026-10-08T12:32:00Z"), paris))
    }

    @Test
    fun padsSingleDigitHoursAndMinutes() {
        assertEquals("07:05", formatClock(Instant.parse("2026-10-08T05:05:00Z"), paris))
    }

    @Test
    fun marksTheoreticalTimesWithATilde() {
        assertEquals("~14:32", clockWithRealtimeMarker(departure(isRealtime = false), paris))
        assertEquals("14:32", clockWithRealtimeMarker(departure(isRealtime = true), paris))
    }

    @Test
    fun delayIsShownInWholeMinutes() {
        assertEquals(3, delayMinutes(departure(delaySeconds = 200)))
    }

    @Test
    fun noDelayNoteBelowOneMinuteOrWhenUnknown() {
        assertNull(delayMinutes(departure(delaySeconds = 59)))
        assertNull(delayMinutes(departure(delaySeconds = 0)))
        assertNull(delayMinutes(departure(delaySeconds = null)))
    }

    @Test
    fun noDelayNoteForCancelledDepartures() {
        assertNull(delayMinutes(departure(delaySeconds = 600, status = DepartureStatus.CANCELLED)))
    }

    @Test
    fun parsesLineColorWithAndWithoutHash() {
        assertEquals(0xFF1F4FA3.toInt(), parseLineColor("#1F4FA3"))
        assertEquals(0xFF1F4FA3.toInt(), parseLineColor(" 1f4fa3 "))
    }

    @Test
    fun rejectsMalformedLineColors() {
        assertNull(parseLineColor(null))
        assertNull(parseLineColor(""))
        assertNull(parseLineColor("#FFF"))
        assertNull(parseLineColor("#GGGGGG"))
        assertNull(parseLineColor("-1F4FA"))
        assertNull(parseLineColor("#1F4FA3FF"))
    }

    @Test
    fun picksDarkTextOnLightBackgroundsAndLightTextOnDarkOnes() {
        assertTrue(prefersDarkText(0xFFFFE000.toInt()))
        assertFalse(prefersDarkText(0xFF1F4FA3.toInt()))
        assertFalse(prefersDarkText(0xFF000000.toInt()))
    }
}
