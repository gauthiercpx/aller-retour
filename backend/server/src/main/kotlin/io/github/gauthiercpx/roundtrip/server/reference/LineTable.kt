package io.github.gauthiercpx.roundtrip.server.reference

import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Vehicle
import io.github.gauthiercpx.roundtrip.model.VehicleConfidence

/** Static line facts the upstream responses do not carry themselves. */
internal object LineTable {
    data class IdfmLine(val name: String, val mode: Mode)

    // PRIM StopMonitoring gives only the LineRef; name and mode come from the IDFM referential
    // (arrets-lignes: C01729 = RER E). Add a line here before using a stop it serves.
    private val idfmLines = mapOf(
        "STIF:Line::C01729:" to IdfmLine(name = "E", mode = Mode.RER),
    )

    // From CLAUDE.md: line a runs VAL, line b runs Cityval. Only line b has appeared in STAR data so far.
    private val starMetroModels = mapOf(
        "a" to "VAL",
        "b" to "CITYVAL",
    )

    fun idfmLine(lineRef: String): IdfmLine? = idfmLines[lineRef]

    fun starMetroVehicle(lineName: String): Vehicle {
        val model = starMetroModels[lineName] ?: return Vehicle.UNKNOWN
        return Vehicle(modelId = model, confidence = VehicleConfidence.EXACT, length = null)
    }
}
