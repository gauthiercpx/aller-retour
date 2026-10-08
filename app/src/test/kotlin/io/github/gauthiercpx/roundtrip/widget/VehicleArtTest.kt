package io.github.gauthiercpx.roundtrip.widget

import io.github.gauthiercpx.roundtrip.R
import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.model.Vehicle
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class VehicleArtTest {
    private fun departure(mode: Mode) = Departure(
        network = Network.IDFM,
        stopId = "idfm:1",
        stopName = "Stop",
        lineId = "L",
        lineName = "E",
        lineColor = null,
        mode = mode,
        direction = null,
        destination = "Somewhere",
        scheduledTime = null,
        expectedTime = Instant.parse("2026-10-08T12:00:00Z"),
        delaySeconds = null,
        status = DepartureStatus.ON_TIME,
        isRealtime = true,
        vehicle = Vehicle.UNKNOWN,
        missionCode = null,
    )

    @Test
    fun railModesShowTheTrainPlaceholder() {
        listOf(Mode.METRO, Mode.RER, Mode.TRAIN, Mode.TRAM).forEach {
            assertEquals(R.drawable.vehicle_placeholder_train, vehicleArt(departure(it)), "mode $it")
        }
    }

    @Test
    fun busesShowTheBusPlaceholder() {
        assertEquals(R.drawable.vehicle_placeholder_bus, vehicleArt(departure(Mode.BUS)))
    }
}
