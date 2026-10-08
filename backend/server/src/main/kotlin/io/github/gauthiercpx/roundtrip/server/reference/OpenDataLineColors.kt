package io.github.gauthiercpx.roundtrip.server.reference

import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.server.cache.CacheResult
import io.github.gauthiercpx.roundtrip.server.cache.TtlCache
import io.github.gauthiercpx.roundtrip.server.upstream.UpstreamJson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Clock
import java.time.Duration

/**
 * Line colours from the operators' open-data portals (ODbL, anonymous, see docs/api-notes/line-colors-and-logos.md):
 * IDFM `referentiel-des-lignes` and STAR's line topology datasets.
 *
 * One line is looked up on first use and kept for a day, so a typical refresh makes no extra upstream call.
 * Colours are cosmetic: any failure yields null (and is remembered for a few minutes) rather than breaking the
 * departures that were already fetched.
 */
class OpenDataLineColors(private val http: HttpClient, clock: Clock) : LineColorSource {
    private val log = LoggerFactory.getLogger(OpenDataLineColors::class.java)

    // A line without a colour is cached too, so it is not asked for again on every refresh.
    private data class Entry(val hex: String?)

    private val cache = TtlCache<LineKey, Entry>(clock, TTL, STALE_RETENTION, FAILURE_BACKOFF)

    override suspend fun color(key: LineKey): String? = when (val result = cache.get(key) { Entry(load(key)) }) {
        is CacheResult.Fresh -> result.value.hex
        is CacheResult.Stale -> result.value.hex
        is CacheResult.Missing -> null
    }

    private suspend fun load(key: LineKey): String? {
        val lookup = lookupFor(key) ?: return null
        try {
            val response = http.get(lookup.url) {
                parameter("where", lookup.where)
                parameter("select", lookup.field)
                parameter("limit", 1)
            }
            if (!response.status.isSuccess()) throw IOException("${lookup.url} answered HTTP ${response.status.value}")
            val record = UpstreamJson.parseToJsonElement(response.bodyAsText())
                .jsonObject["results"]?.jsonArray?.firstOrNull()?.jsonObject
            return normalizeHex(record?.get(lookup.field)?.jsonPrimitive?.contentOrNull)
        } catch (e: IOException) {
            log.warn("line colour for {}: {}", key, e.toString())
            throw e
        }
    }

    private class Lookup(val url: String, val where: String, val field: String)

    // Ids end up inside an ODSQL `where` clause, so only the exact shapes the upstreams use are let through.
    private fun lookupFor(key: LineKey): Lookup? = when (key.network) {
        Network.IDFM -> IDFM_LINE_REF.matchEntire(key.lineId)?.let {
            Lookup(IDFM_URL, "id_line=\"${it.groupValues[1]}\"", "colourweb_hexa")
        }
        Network.STAR -> if (STAR_LINE_ID.matches(key.lineId)) {
            val dataset = if (key.mode == Mode.BUS) STAR_BUS_DATASET else STAR_METRO_DATASET
            Lookup("$STAR_DATASETS_URL/$dataset/records", "id=\"${key.lineId}\"", "couleurligne")
        } else {
            null
        }
        Network.SNCF -> null
    }

    /** IDFM publishes `b94e9a`, STAR `#00893e`; both become `#rrggbb`. Anything else is treated as no colour. */
    private fun normalizeHex(raw: String?): String? = raw?.trim()?.takeIf { HEX_COLOR.matches(it) }
        ?.removePrefix("#")?.lowercase()?.let { "#$it" }

    private companion object {
        val TTL: Duration = Duration.ofHours(24)
        val STALE_RETENTION: Duration = Duration.ofDays(7)
        val FAILURE_BACKOFF: Duration = Duration.ofMinutes(5)

        const val IDFM_URL =
            "https://data.iledefrance-mobilites.fr/api/explore/v2.1/catalog/datasets/referentiel-des-lignes/records"
        const val STAR_DATASETS_URL = "https://data.explore.star.fr/api/explore/v2.1/catalog/datasets"
        const val STAR_METRO_DATASET = "tco-metro-topologie-lignes-td"
        const val STAR_BUS_DATASET = "tco-bus-topologie-lignes-td"

        // `STIF:Line::C01729:` -> id_line C01729
        val IDFM_LINE_REF = Regex("""STIF:Line::([A-Z0-9]{1,12}):""")
        val STAR_LINE_ID = Regex("""\d{1,6}""")
        val HEX_COLOR = Regex("""#?[0-9a-fA-F]{6}""")
    }
}
