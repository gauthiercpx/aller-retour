package io.github.gauthiercpx.roundtrip.server.upstream.star

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.model.Vehicle
import io.github.gauthiercpx.roundtrip.model.VehicleConfidence
import io.github.gauthiercpx.roundtrip.server.Fixtures
import io.github.gauthiercpx.roundtrip.server.StopId
import io.github.gauthiercpx.roundtrip.server.StopSource
import io.github.gauthiercpx.roundtrip.server.upstream.UpstreamDataException
import io.github.gauthiercpx.roundtrip.server.upstream.UpstreamJson
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StarMapperTest {
    private val metroStop = StopId(StopSource.STAR_METRO, "5074")
    private val metroDepartures = StarMapper.metro(
        UpstreamJson.decodeFromString<ExploreRecords<MetroPassage>>(Fixtures.read(Fixtures.STAR_METRO_GROS_CHENE)),
        metroStop,
    )

    private val busStop = StopId(StopSource.STAR_BUS, "1317")
    private val busDepartures = StarMapper.bus(
        UpstreamJson.decodeFromString<ExploreRecords<BusPassage>>(Fixtures.read(Fixtures.STAR_BUS_PASSAGES)),
        busStop,
    )

    @Test
    fun keepsOnlyMetroRowsForTheRequestedStop() {
        // The sample holds rows for both Gros-Chêne platforms: 24 for 5074 and 26 for 5055.
        assertEquals(24, metroDepartures.size)
    }

    @Test
    fun mapsAllFieldsOfARealMetroRow() {
        val departure = metroDepartures.single { it.expectedTime == Instant.parse("2026-10-08T11:58:59Z") }

        assertEquals(
            Departure(
                network = Network.STAR,
                stopId = "star-metro:5074",
                stopName = "Gros-Chêne",
                lineId = "1002",
                lineName = "b",
                lineColor = null,
                mode = Mode.METRO,
                direction = "1",
                destination = "Cesson - Viasilva",
                scheduledTime = null,
                expectedTime = Instant.parse("2026-10-08T11:58:59Z"),
                delaySeconds = null,
                status = DepartureStatus.UNKNOWN,
                isRealtime = true,
                vehicle = Vehicle(modelId = "CITYVAL", confidence = VehicleConfidence.EXACT, length = null),
                missionCode = null,
            ),
            departure,
        )
    }

    @Test
    fun flagsOnlyTempsReelMetroRowsAsRealtime() {
        val realtimeTimes = metroDepartures.filter { it.isRealtime }.map { it.expectedTime }

        assertEquals(
            listOf(Instant.parse("2026-10-08T11:58:59Z"), Instant.parse("2026-10-08T12:01:21Z")),
            realtimeTimes,
        )
    }

    @Test
    fun mapsAllFieldsOfARealBusRowIncludingDelay() {
        assertEquals(
            listOf(
                Departure(
                    network = Network.STAR,
                    stopId = "star-bus:1317",
                    stopName = "Villebois-Mareuil",
                    lineId = "0001",
                    lineName = "C1",
                    lineColor = null,
                    mode = Mode.BUS,
                    direction = "0",
                    destination = "Chantepie",
                    scheduledTime = Instant.parse("2026-10-08T11:39:00Z"),
                    expectedTime = Instant.parse("2026-10-08T11:52:33Z"),
                    delaySeconds = 813,
                    status = DepartureStatus.UNKNOWN,
                    isRealtime = true,
                    vehicle = Vehicle.UNKNOWN,
                    missionCode = null,
                ),
            ),
            busDepartures,
        )
    }

    @Test
    fun returnsNoDeparturesWhenTheStopHasNoRows() {
        val departures = StarMapper.metro(
            UpstreamJson.decodeFromString<ExploreRecords<MetroPassage>>(Fixtures.read(Fixtures.STAR_METRO_GROS_CHENE)),
            StopId(StopSource.STAR_METRO, "9999"),
        )

        assertEquals(emptyList(), departures)
    }

    @Test
    fun throwsWhenNoRowForTheStopIsMappable() {
        // `depart` in the local-time format that `arrivee` uses, which the mapper does not accept.
        val json = """{"results":[{"idligne":"1002","nomcourtligne":"b","sens":1,"destination":"Cesson - Viasilva",
            "idarret":"5074","nomarret":"Gros-Chêne","depart":"2026-10-08 13:58:41+0200","precision":["Temps réel"]}]}"""

        assertFailsWith<UpstreamDataException> {
            StarMapper.metro(UpstreamJson.decodeFromString<ExploreRecords<MetroPassage>>(json), metroStop)
        }
    }

    @Test
    fun throwsWhenResultsArrayIsMissing() {
        val json = """{"error_code":"ODSQLError","message":"invalid query"}"""

        assertFailsWith<UpstreamDataException> {
            StarMapper.bus(UpstreamJson.decodeFromString<ExploreRecords<BusPassage>>(json), busStop)
        }
    }
}
