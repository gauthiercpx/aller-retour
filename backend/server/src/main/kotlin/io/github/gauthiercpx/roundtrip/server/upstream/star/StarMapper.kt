package io.github.gauthiercpx.roundtrip.server.upstream.star

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.model.Vehicle
import io.github.gauthiercpx.roundtrip.server.StopId
import io.github.gauthiercpx.roundtrip.server.reference.LineTable
import io.github.gauthiercpx.roundtrip.server.upstream.UpstreamDataException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

internal object StarMapper {
    private val log = LoggerFactory.getLogger(StarMapper::class.java)

    /** `precision` value for a live prediction; anything else ("Applicable") is the theoretical schedule. */
    private const val REALTIME = "Temps réel"

    fun metro(records: ExploreRecords<MetroPassage>, stop: StopId): List<Departure> =
        mapRows(records, stop, MetroPassage::idarret) { row -> metroDeparture(row, stop) }

    fun bus(records: ExploreRecords<BusPassage>, stop: StopId): List<Departure> =
        mapRows(records, stop, BusPassage::idarret) { row -> busDeparture(row, stop) }

    /**
     * No rows for the stop is a valid empty answer (no service). Rows that all fail to map, or a missing
     * `results` array, throw [UpstreamDataException] so the cache keeps serving the last good departures.
     */
    private fun <T> mapRows(
        records: ExploreRecords<T>,
        stop: StopId,
        stopIdOf: (T) -> String?,
        toDeparture: (T) -> Departure?,
    ): List<Departure> {
        val rows = records.results
            ?: throw UpstreamDataException(StarClient.UPSTREAM_NAME, "no results array for $stop")
        // The query already filters on idarret; filtering again guards against an upstream that ignores it.
        val stopRows = rows.filter { stopIdOf(it) == stop.localId }
        val departures = stopRows.mapNotNull(toDeparture)

        val dropped = stopRows.size - departures.size
        if (dropped > 0) {
            val detail = "$dropped of ${stopRows.size} rows unmappable for $stop (missing or unparseable fields)"
            if (departures.isEmpty()) throw UpstreamDataException(StarClient.UPSTREAM_NAME, detail)
            log.warn("Dropped {}", detail)
        }
        return departures
    }

    private fun metroDeparture(row: MetroPassage, stop: StopId): Departure? {
        val lineName = row.nomcourtligne ?: return null
        return Departure(
            network = Network.STAR,
            stopId = stop.toString(),
            stopName = row.nomarret ?: return null,
            lineId = row.idligne ?: return null,
            lineName = lineName,
            lineColor = null,
            mode = Mode.METRO,
            direction = row.sens?.toString(),
            destination = row.destination ?: return null,
            // The metro dataset gives a single predicted time: no schedule, delay or status.
            scheduledTime = null,
            expectedTime = parseInstant(row.depart) ?: return null,
            delaySeconds = null,
            status = DepartureStatus.UNKNOWN,
            isRealtime = row.precision.orEmpty().contains(REALTIME),
            vehicle = LineTable.starMetroVehicle(lineName),
            missionCode = null,
        )
    }

    private fun busDeparture(row: BusPassage, stop: StopId): Departure? {
        val expected = parseInstant(row.depart) ?: return null
        val scheduled = parseInstant(row.departtheorique)
        return Departure(
            network = Network.STAR,
            stopId = stop.toString(),
            stopName = row.nomarret ?: return null,
            lineId = row.idligne ?: return null,
            lineName = row.nomcourtligne ?: return null,
            lineColor = null,
            mode = Mode.BUS,
            direction = row.sens?.toString(),
            destination = row.destination ?: return null,
            scheduledTime = scheduled,
            expectedTime = expected,
            delaySeconds = scheduled?.let { Duration.between(it, expected).seconds.toInt() },
            status = DepartureStatus.UNKNOWN,
            isRealtime = row.precision == REALTIME,
            // Bus model needs the GTFS-RT vehicle join, which is not part of the MVP.
            vehicle = Vehicle.UNKNOWN,
            missionCode = null,
        )
    }

    private fun parseInstant(raw: String?): Instant? {
        if (raw == null) return null
        return try {
            OffsetDateTime.parse(raw).toInstant()
        } catch (e: DateTimeParseException) {
            log.warn("Unparseable STAR timestamp '{}': {}", raw, e.message)
            null
        }
    }
}
