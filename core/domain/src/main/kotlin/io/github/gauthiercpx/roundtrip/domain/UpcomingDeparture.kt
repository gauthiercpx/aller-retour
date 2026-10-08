package io.github.gauthiercpx.roundtrip.domain

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import io.github.gauthiercpx.roundtrip.model.StopState
import java.time.Duration
import java.time.Instant

/** A departure plus whether its stop's data was not fresh when the backend answered. */
data class UpcomingDeparture(val departure: Departure, val isStale: Boolean)

/** Departures that just left stay visible this long, since the widget is only refreshed every 15 minutes. */
private val DEPARTED_GRACE = Duration.ofMinutes(1)

fun selectUpcoming(response: DeparturesResponse, now: Instant, count: Int): List<UpcomingDeparture> = response.stops
    .flatMap { stop -> stop.departures.map { UpcomingDeparture(it, isStale = stop.state != StopState.FRESH) } }
    .filter { it.departure.expectedTime >= now.minus(DEPARTED_GRACE) }
    .sortedBy { it.departure.expectedTime }
    .take(count)
