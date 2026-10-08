package io.github.gauthiercpx.roundtrip.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class DepartureSerializationTest {
    private val departure = Departure(
        network = Network.IDFM,
        stopId = "idfm:58572",
        stopName = "Magenta",
        lineId = "STIF:Line::C01729:",
        lineName = "E",
        lineColor = null,
        mode = Mode.RER,
        direction = "Aller",
        destination = "Tournan",
        scheduledTime = Instant.parse("2026-10-08T11:55:30Z"),
        expectedTime = Instant.parse("2026-10-08T11:56:23Z"),
        delaySeconds = 53,
        status = DepartureStatus.ON_TIME,
        isRealtime = true,
        vehicle = Vehicle(modelId = null, confidence = VehicleConfidence.UNKNOWN, length = VehicleLength.LONG),
        missionCode = "TANU",
    )

    @Test
    fun encodesInstantsAsIsoUtcStrings() {
        val json = Json.encodeToJsonElement(Departure.serializer(), departure).jsonObject

        assertEquals("2026-10-08T11:56:23Z", json.getValue("expectedTime").jsonPrimitive.content)
    }

    @Test
    fun roundTripsThroughJson() {
        val encoded = Json.encodeToString(Departure.serializer(), departure)

        val decoded = Json.decodeFromString(Departure.serializer(), encoded)

        assertEquals(departure, decoded)
    }
}
