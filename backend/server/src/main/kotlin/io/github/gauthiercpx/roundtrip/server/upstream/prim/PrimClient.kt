package io.github.gauthiercpx.roundtrip.server.upstream.prim

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.server.StopId
import io.github.gauthiercpx.roundtrip.server.upstream.MissingConfigException
import io.github.gauthiercpx.roundtrip.server.upstream.UpstreamJson
import io.github.gauthiercpx.roundtrip.server.upstream.ensureSuccess
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

/** IDFM PRIM "Prochains Passages" (SIRI StopMonitoring), one unit request per StopArea. */
class PrimClient(
    private val http: HttpClient,
    private val apiKey: String?,
) {
    suspend fun departures(stop: StopId): List<Departure> {
        val key = apiKey ?: throw MissingConfigException("PRIM_API_KEY")

        val response = http.get(STOP_MONITORING_URL) {
            parameter("MonitoringRef", "STIF:StopArea:SP:${stop.localId}:")
            header("apikey", key)
        }
        response.ensureSuccess(UPSTREAM_NAME)

        val body = UpstreamJson.decodeFromString<PrimStopMonitoringResponse>(response.bodyAsText())
        return PrimMapper.map(body, stop)
    }

    companion object {
        const val UPSTREAM_NAME = "prim"
        const val STOP_MONITORING_URL = "https://prim.iledefrance-mobilites.fr/marketplace/stop-monitoring"
    }
}
