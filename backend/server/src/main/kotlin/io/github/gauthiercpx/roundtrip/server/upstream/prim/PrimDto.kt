package io.github.gauthiercpx.roundtrip.server.upstream.prim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// SIRI StopMonitoring as returned by PRIM. Only the mapped fields are declared, all nullable:
// see docs/api-notes/prim-stop-monitoring.md for what the real response contains.

@Serializable
internal data class PrimStopMonitoringResponse(
    @SerialName("Siri") val siri: Siri? = null,
)

@Serializable
internal data class Siri(
    @SerialName("ServiceDelivery") val serviceDelivery: ServiceDelivery? = null,
)

@Serializable
internal data class ServiceDelivery(
    @SerialName("StopMonitoringDelivery") val stopMonitoringDelivery: List<StopMonitoringDelivery>? = null,
)

@Serializable
internal data class StopMonitoringDelivery(
    /** A string in PRIM's JSON ("true" in the sample), not a boolean. "false" is SIRI's producer-error signal. */
    @SerialName("Status") val status: String? = null,
    @SerialName("MonitoredStopVisit") val monitoredStopVisit: List<MonitoredStopVisit>? = null,
)

@Serializable
internal data class MonitoredStopVisit(
    @SerialName("MonitoredVehicleJourney") val journey: MonitoredVehicleJourney? = null,
)

@Serializable
internal data class MonitoredVehicleJourney(
    @SerialName("LineRef") val lineRef: SiriValue? = null,
    @SerialName("DirectionRef") val directionRef: SiriValue? = null,
    @SerialName("DestinationName") val destinationName: List<SiriValue>? = null,
    @SerialName("JourneyNote") val journeyNote: List<SiriValue>? = null,
    @SerialName("VehicleFeatureRef") val vehicleFeatureRef: List<String>? = null,
    @SerialName("MonitoredCall") val monitoredCall: MonitoredCall? = null,
)

@Serializable
internal data class MonitoredCall(
    @SerialName("StopPointName") val stopPointName: List<SiriValue>? = null,
    @SerialName("AimedDepartureTime") val aimedDepartureTime: String? = null,
    @SerialName("ExpectedDepartureTime") val expectedDepartureTime: String? = null,
    @SerialName("DepartureStatus") val departureStatus: String? = null,
)

@Serializable
internal data class SiriValue(
    val value: String? = null,
)
