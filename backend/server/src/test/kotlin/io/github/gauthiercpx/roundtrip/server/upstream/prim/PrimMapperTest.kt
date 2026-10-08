package io.github.gauthiercpx.roundtrip.server.upstream.prim

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.model.Vehicle
import io.github.gauthiercpx.roundtrip.model.VehicleConfidence
import io.github.gauthiercpx.roundtrip.model.VehicleLength
import io.github.gauthiercpx.roundtrip.server.Fixtures
import io.github.gauthiercpx.roundtrip.server.StopId
import io.github.gauthiercpx.roundtrip.server.StopSource
import io.github.gauthiercpx.roundtrip.server.upstream.UpstreamDataException
import io.github.gauthiercpx.roundtrip.server.upstream.UpstreamJson
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrimMapperTest {
    private val stop = StopId(StopSource.IDFM, "58572")

    private fun mapJson(json: String): List<Departure> =
        PrimMapper.map(UpstreamJson.decodeFromString<PrimStopMonitoringResponse>(json), stop)

    private val sampleDepartures = mapJson(Fixtures.read(Fixtures.PRIM_MAGENTA))

    @Test
    fun mapsEveryVisitInTheMagentaSample() {
        assertEquals(65, sampleDepartures.size)
    }

    @Test
    fun mapsAllFieldsOfARealVisit() {
        val departure = sampleDepartures.single { it.expectedTime == Instant.parse("2026-10-08T11:56:23Z") }

        assertEquals(
            Departure(
                network = Network.IDFM,
                stopId = "idfm:58572",
                stopName = "Magenta",
                lineId = "STIF:Line::C01729:",
                lineName = "E",
                lineColor = null,
                mode = Mode.RER,
                direction = "Aller",
                destination = "Villiers-sur-Marne - Le Plessis-Trévise",
                scheduledTime = Instant.parse("2026-10-08T11:55:30Z"),
                expectedTime = Instant.parse("2026-10-08T11:56:23Z"),
                delaySeconds = 53,
                status = DepartureStatus.ON_TIME,
                isRealtime = true,
                vehicle = Vehicle(modelId = null, confidence = VehicleConfidence.UNKNOWN, length = VehicleLength.LONG),
                missionCode = "VONY",
            ),
            departure,
        )
    }

    @Test
    fun returnsNoDeparturesWhenTheStopHasNoVisits() {
        val departures = mapJson(document())

        assertTrue(departures.isEmpty())
    }

    @Test
    fun keepsKnownLinesWhenOtherVisitsAreOnUnknownLines() {
        val departures = mapJson(document(visit(), visit(lineRef = "STIF:Line::C99999:")))

        assertEquals(listOf("STIF:Line::C01729:"), departures.map { it.lineId })
    }

    @Test
    fun throwsWhenNoVisitIsOnAKnownLine() {
        assertFailsWith<UpstreamDataException> { mapJson(document(visit(lineRef = "STIF:Line::C99999:"))) }
    }

    @Test
    fun throwsWhenDeliveryStatusIsFalse() {
        assertFailsWith<UpstreamDataException> { mapJson(document(visit(), deliveryStatus = "false")) }
    }

    @Test
    fun throwsWhenServiceDeliveryIsMissing() {
        assertFailsWith<UpstreamDataException> { mapJson("""{"Siri":{}}""") }
    }

    @Test
    fun mapsDelayedStatus() {
        val departures = mapJson(document(visit(status = "delayed")))

        assertEquals(DepartureStatus.DELAYED, departures.single().status)
    }

    @Test
    fun mapsCancelledStatus() {
        val departures = mapJson(document(visit(status = "cancelled")))

        assertEquals(DepartureStatus.CANCELLED, departures.single().status)
    }

    @Test
    fun mapsUnrecognisedStatusToUnknown() {
        val departures = mapJson(document(visit(status = "noReport")))

        assertEquals(DepartureStatus.UNKNOWN, departures.single().status)
    }

    @Test
    fun leavesLengthUnsetWithoutLongTrainFeature() {
        val departures = mapJson(document(visit(features = "[]")))

        assertNull(departures.single().vehicle.length)
    }

    @Test
    fun fallsBackToScheduledTimeWhenExpectedTimeIsMissing() {
        val departure = mapJson(document(visit(expectedDeparture = null))).single()

        assertEquals(Instant.parse("2026-10-08T12:00:00Z"), departure.expectedTime)
        assertFalse(departure.isRealtime)
        assertNull(departure.delaySeconds)
    }

    private fun visit(
        lineRef: String = "STIF:Line::C01729:",
        status: String = "onTime",
        features: String = """["longTrain"]""",
        expectedDeparture: String? = "2026-10-08T12:01:00.000Z",
    ): String {
        val expectedField = expectedDeparture?.let { ""","ExpectedDepartureTime":"$it"""" } ?: ""
        return """
            {"MonitoredVehicleJourney":{
              "LineRef":{"value":"$lineRef"},
              "DirectionRef":{"value":"Aller"},
              "DestinationName":[{"value":"Tournan"}],
              "JourneyNote":[{"value":"TANU"}],
              "VehicleFeatureRef":$features,
              "MonitoredCall":{
                "StopPointName":[{"value":"Magenta"}],
                "AimedDepartureTime":"2026-10-08T12:00:00.000Z"$expectedField,
                "DepartureStatus":"$status"
              }
            }}
        """.trimIndent()
    }

    private fun document(vararg visits: String, deliveryStatus: String = "true"): String =
        """{"Siri":{"ServiceDelivery":{"StopMonitoringDelivery":[""" +
            """{"Status":"$deliveryStatus","MonitoredStopVisit":[${visits.joinToString(",")}]}]}}}"""
}
