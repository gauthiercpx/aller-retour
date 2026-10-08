package io.github.gauthiercpx.roundtrip.server.upstream.prim

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.model.Vehicle
import io.github.gauthiercpx.roundtrip.model.VehicleConfidence
import io.github.gauthiercpx.roundtrip.model.VehicleLength
import io.github.gauthiercpx.roundtrip.server.StopId
import io.github.gauthiercpx.roundtrip.server.reference.LineTable
import io.github.gauthiercpx.roundtrip.server.upstream.UpstreamDataException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

internal object PrimMapper {
    private val log = LoggerFactory.getLogger(PrimMapper::class.java)

    private const val LONG_TRAIN_FEATURE = "longTrain"

    /**
     * Throws [UpstreamDataException] when the response is unusable (error flag, no envelope, or no visit
     * mappable) so the cache keeps serving the last good departures instead of an empty "fresh" list.
     */
    fun map(response: PrimStopMonitoringResponse, stop: StopId): List<Departure> {
        val deliveries = response.siri?.serviceDelivery?.stopMonitoringDelivery
            ?: throw UpstreamDataException(PrimClient.UPSTREAM_NAME, "no Siri.ServiceDelivery for $stop")
        if (deliveries.any { it.status == "false" }) {
            throw UpstreamDataException(PrimClient.UPSTREAM_NAME, "StopMonitoringDelivery Status is false for $stop")
        }

        val journeys = deliveries.flatMap { it.monitoredStopVisit.orEmpty() }.mapNotNull { it.journey }
        val departures = journeys.mapNotNull { toDeparture(it, stop) }

        val dropped = journeys.size - departures.size
        if (dropped > 0) {
            val unknownLines = journeys.mapNotNull { it.lineRef?.value }.filter { LineTable.idfmLine(it) == null }.toSet()
            val detail = "$dropped of ${journeys.size} visits unmappable for $stop (lines missing from LineTable: $unknownLines)"
            if (departures.isEmpty()) throw UpstreamDataException(PrimClient.UPSTREAM_NAME, detail)
            log.warn("Dropped {}", detail)
        }
        return departures
    }

    private fun toDeparture(journey: MonitoredVehicleJourney, stop: StopId): Departure? {
        val call = journey.monitoredCall ?: return null
        val lineRef = journey.lineRef?.value ?: return null
        val line = LineTable.idfmLine(lineRef) ?: return null
        val destination = journey.destinationName?.firstOrNull()?.value ?: return null
        val stopName = call.stopPointName?.firstOrNull()?.value ?: return null
        val scheduled = parseInstant(call.aimedDepartureTime)
        // How PRIM represents a cancellation is not observed yet. SIRI producers often omit the expected time
        // then, so fall back to the schedule (flagged as not realtime) rather than dropping the train.
        val realtime = parseInstant(call.expectedDepartureTime)
        val expected = realtime ?: scheduled ?: return null
        val isLongTrain = journey.vehicleFeatureRef.orEmpty().contains(LONG_TRAIN_FEATURE)

        return Departure(
            network = Network.IDFM,
            stopId = stop.toString(),
            stopName = stopName,
            lineId = lineRef,
            lineName = line.name,
            lineColor = null,
            mode = line.mode,
            direction = journey.directionRef?.value,
            destination = destination,
            scheduledTime = scheduled,
            expectedTime = expected,
            delaySeconds = if (realtime != null && scheduled != null) Duration.between(scheduled, realtime).seconds.toInt() else null,
            status = mapStatus(call.departureStatus),
            isRealtime = realtime != null,
            // PRIM exposes no rolling-stock model; the only vehicle fact is the length feature.
            vehicle = Vehicle(
                modelId = null,
                confidence = VehicleConfidence.UNKNOWN,
                length = if (isLongTrain) VehicleLength.LONG else null,
            ),
            missionCode = journey.journeyNote?.firstOrNull()?.value,
        )
    }

    private fun mapStatus(raw: String?): DepartureStatus = when (raw) {
        "onTime" -> DepartureStatus.ON_TIME
        // Only "onTime" has been seen from PRIM; these two follow the SIRI enum and are unverified.
        "delayed" -> DepartureStatus.DELAYED
        "cancelled" -> DepartureStatus.CANCELLED
        else -> DepartureStatus.UNKNOWN
    }

    private fun parseInstant(raw: String?): Instant? {
        if (raw == null) return null
        return try {
            Instant.parse(raw)
        } catch (e: DateTimeParseException) {
            log.warn("Unparseable PRIM timestamp '{}': {}", raw, e.message)
            null
        }
    }
}
