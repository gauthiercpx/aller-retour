package io.github.gauthiercpx.roundtrip.server

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.model.Vehicle
import io.github.gauthiercpx.roundtrip.server.cache.TtlCache
import io.github.gauthiercpx.roundtrip.server.ratelimit.TokenBucket
import kotlinx.coroutines.test.runTest
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class DepartureServiceTest {
    private val now = Instant.parse("2026-10-08T12:00:00Z")
    private val clock = MutableClock(now)
    private val stop = StopId(StopSource.STAR_METRO, "5074")

    private fun serviceReturning(departures: List<Departure>) = DepartureService(
        upstreams = mapOf(
            StopSource.STAR_METRO to GuardedUpstream(
                name = "test",
                limiter = TokenBucket(clock, capacity = 10, refillInterval = Duration.ofSeconds(1)),
                cache = TtlCache(
                    clock,
                    ttl = Duration.ofSeconds(30),
                    staleRetention = Duration.ofMinutes(10),
                    failureBackoff = Duration.ofSeconds(30),
                ),
                fetch = { departures },
            ),
        ),
        clock = clock,
    )

    @Test
    fun dropsDeparturesThatLeftMoreThanAMinuteAgo() = runTest {
        val service = serviceReturning(
            listOf(departureAt("11:58:59"), departureAt("11:59:00"), departureAt("12:05:00")),
        )

        val times = service.departures(listOf(stop)).stops.single().departures.map { it.expectedTime }

        assertEquals(listOf(Instant.parse("2026-10-08T11:59:00Z"), Instant.parse("2026-10-08T12:05:00Z")), times)
    }

    @Test
    fun sortsDeparturesByExpectedTime() = runTest {
        val service = serviceReturning(listOf(departureAt("12:10:00"), departureAt("12:02:00")))

        val times = service.departures(listOf(stop)).stops.single().departures.map { it.expectedTime }

        assertEquals(listOf(Instant.parse("2026-10-08T12:02:00Z"), Instant.parse("2026-10-08T12:10:00Z")), times)
    }

    private fun departureAt(time: String) = Departure(
        network = Network.STAR,
        stopId = stop.toString(),
        stopName = "Gros-Chêne",
        lineId = "1002",
        lineName = "b",
        lineColor = null,
        mode = Mode.METRO,
        direction = "1",
        destination = "Cesson - Viasilva",
        scheduledTime = null,
        expectedTime = Instant.parse("2026-10-08T${time}Z"),
        delaySeconds = null,
        status = DepartureStatus.UNKNOWN,
        isRealtime = true,
        vehicle = Vehicle.UNKNOWN,
        missionCode = null,
    )
}
