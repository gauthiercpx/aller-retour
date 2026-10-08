package io.github.gauthiercpx.roundtrip.server.upstream.star

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.server.StopId
import io.github.gauthiercpx.roundtrip.server.upstream.UpstreamJson
import io.github.gauthiercpx.roundtrip.server.upstream.ensureSuccess
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

/** STAR Rennes real-time passages through the Opendatasoft Explore v2.1 API. No key needed. */
class StarClient(
    private val http: HttpClient,
) {
    suspend fun metroDepartures(stop: StopId): List<Departure> =
        StarMapper.metro(UpstreamJson.decodeFromString(fetchRecords(METRO_DATASET, stop)), stop)

    suspend fun busDepartures(stop: StopId): List<Departure> =
        StarMapper.bus(UpstreamJson.decodeFromString(fetchRecords(BUS_DATASET, stop)), stop)

    private suspend fun fetchRecords(dataset: String, stop: StopId): String {
        val response = http.get("$EXPLORE_DATASETS_URL/$dataset/records") {
            // stop.localId is digits only (validated in StopId), so it is safe inside the ODSQL clause.
            parameter("where", "idarret=\"${stop.localId}\"")
            parameter("order_by", "depart")
            parameter("limit", RECORD_LIMIT)
        }
        response.ensureSuccess(UPSTREAM_NAME)
        return response.bodyAsText()
    }

    companion object {
        const val UPSTREAM_NAME = "star"
        const val EXPLORE_DATASETS_URL = "https://data.explore.star.fr/api/explore/v2.1/catalog/datasets"
        const val METRO_DATASET = "tco-metro-circulation-passages-tr"
        const val BUS_DATASET = "tco-bus-circulation-passages-tr"
        private const val RECORD_LIMIT = 20
    }
}
