package io.github.gauthiercpx.roundtrip.domain

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.model.StopResult
import io.github.gauthiercpx.roundtrip.model.StopState
import io.github.gauthiercpx.roundtrip.model.Vehicle
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpcomingDeparturesTest {
    private val now = Instant.parse("2026-10-08T08:00:00Z")

    private fun departure(destination: String, expected: String) = Departure(
        network = Network.IDFM,
        stopId = "idfm:1",
        stopName = "Stop",
        lineId = "L",
        lineName = "E",
        lineColor = null,
        mode = Mode.RER,
        direction = null,
        destination = destination,
        scheduledTime = null,
        expectedTime = Instant.parse(expected),
        delaySeconds = null,
        status = DepartureStatus.ON_TIME,
        isRealtime = true,
        vehicle = Vehicle.UNKNOWN,
        missionCode = null,
    )

    private fun stop(state: StopState, vararg departures: Departure) =
        StopResult("idfm:1", state, fetchedAt = now, departures = departures.toList())

    private fun response(vararg stops: StopResult) = DeparturesResponse(generatedAt = now, stops = stops.toList())

    @Test
    fun mergesStopsAndSortsByExpectedTime() {
        val result = selectUpcoming(
            response(
                stop(StopState.FRESH, departure("B", "2026-10-08T08:10:00Z")),
                stop(StopState.FRESH, departure("A", "2026-10-08T08:05:00Z")),
            ),
            now,
            count = 5,
        )

        assertEquals(listOf("A", "B"), result.map { it.departure.destination })
    }

    @Test
    fun limitsToTheRequestedCount() {
        val result = selectUpcoming(
            response(
                stop(
                    StopState.FRESH,
                    departure("A", "2026-10-08T08:01:00Z"),
                    departure("B", "2026-10-08T08:02:00Z"),
                    departure("C", "2026-10-08T08:03:00Z"),
                ),
            ),
            now,
            count = 2,
        )

        assertEquals(listOf("A", "B"), result.map { it.departure.destination })
    }

    @Test
    fun dropsDeparturesGonePastTheGracePeriodButKeepsTheJustLeftOnes() {
        val result = selectUpcoming(
            response(
                stop(
                    StopState.FRESH,
                    departure("gone", "2026-10-08T07:58:59Z"),
                    departure("justLeft", "2026-10-08T07:59:30Z"),
                ),
            ),
            now,
            count = 5,
        )

        assertEquals(listOf("justLeft"), result.map { it.departure.destination })
    }

    @Test
    fun marksDeparturesFromNonFreshStopsAsStale() {
        val result = selectUpcoming(
            response(
                stop(StopState.STALE, departure("A", "2026-10-08T08:05:00Z")),
                stop(StopState.FRESH, departure("B", "2026-10-08T08:06:00Z")),
            ),
            now,
            count = 5,
        )

        assertEquals(listOf(true, false), result.map { it.isStale })
    }

    @Test
    fun returnsNothingWhenNoStopHasDepartures() {
        assertTrue(selectUpcoming(response(stop(StopState.UNAVAILABLE)), now, count = 2).isEmpty())
    }
}
