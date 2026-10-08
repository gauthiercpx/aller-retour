@file:UseSerializers(InstantSerializer::class)

package io.github.gauthiercpx.roundtrip.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

enum class Network { STAR, IDFM, SNCF }

enum class Mode { METRO, RER, TRAIN, TRAM, BUS }

/** UNKNOWN when the upstream gives no status at all (STAR passages). */
enum class DepartureStatus { ON_TIME, DELAYED, CANCELLED, UNKNOWN }

enum class VehicleConfidence { EXACT, PROBABLE, UNKNOWN }

enum class VehicleLength { SHORT, LONG }

@Serializable
data class Vehicle(
    val modelId: String?,
    val confidence: VehicleConfidence,
    val length: VehicleLength?,
) {
    companion object {
        val UNKNOWN = Vehicle(modelId = null, confidence = VehicleConfidence.UNKNOWN, length = null)
    }
}

/**
 * One departure, normalised across networks.
 *
 * Nullable fields are the ones at least one upstream does not provide: STAR metro has no scheduled
 * time (so no delay), and no source observed so far carries a line color.
 */
@Serializable
data class Departure(
    val network: Network,
    val stopId: String,
    val stopName: String,
    val lineId: String,
    val lineName: String,
    val lineColor: String?,
    val mode: Mode,
    /** Upstream direction code: PRIM `DirectionRef` ("Aller"/"Retour"), STAR `sens` ("0"/"1"). */
    val direction: String?,
    val destination: String,
    val scheduledTime: Instant?,
    val expectedTime: Instant,
    val delaySeconds: Int?,
    val status: DepartureStatus,
    /** False when the time is only the theoretical schedule (STAR "Applicable"). */
    val isRealtime: Boolean,
    val vehicle: Vehicle,
    val missionCode: String?,
    val alerts: List<String> = emptyList(),
)

enum class StopState { FRESH, STALE, UNAVAILABLE }

@Serializable
data class StopResult(
    val stopId: String,
    val state: StopState,
    val fetchedAt: Instant?,
    val departures: List<Departure>,
)

@Serializable
data class DeparturesResponse(
    val generatedAt: Instant,
    val stops: List<StopResult>,
)
