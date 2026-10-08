package io.github.gauthiercpx.roundtrip.server

/** Which upstream dataset a stop belongs to, chosen by the prefix in the request. */
enum class StopSource(val prefix: String) {
    IDFM("idfm"),
    STAR_METRO("star-metro"),
    STAR_BUS("star-bus"),
}

/**
 * A validated stop, e.g. `idfm:58572` or `star-metro:5074`.
 *
 * [localId] is digits only; upstream queries embed it directly (PRIM `MonitoringRef`, STAR ODSQL `where`),
 * so this validation is what keeps those queries injection-free.
 */
data class StopId(val source: StopSource, val localId: String) {
    override fun toString(): String = "${source.prefix}:$localId"
}

class InvalidStopsException(message: String) : IllegalArgumentException(message)

const val MAX_STOPS_PER_REQUEST = 10

private val LOCAL_ID = Regex("""\d{1,9}""")
private val SOURCES_BY_PREFIX = StopSource.entries.associateBy { it.prefix }

/** Parses the comma-separated `stops` query parameter, de-duplicating while keeping request order. */
fun parseStopIds(raw: String?): List<StopId> {
    if (raw.isNullOrBlank()) throw InvalidStopsException("Query parameter 'stops' is required")

    val stops = raw.split(',').mapIndexed { index, token ->
        parseStopId(token.trim()) ?: throw InvalidStopsException(
            "Stop #${index + 1} is invalid: expected <prefix>:<digits> with prefix " +
                StopSource.entries.joinToString(", ") { it.prefix },
        )
    }.distinct()

    if (stops.size > MAX_STOPS_PER_REQUEST) {
        throw InvalidStopsException("At most $MAX_STOPS_PER_REQUEST stops per request, got ${stops.size}")
    }
    return stops
}

private fun parseStopId(token: String): StopId? {
    val prefix = token.substringBefore(':', missingDelimiterValue = "")
    val localId = token.substringAfter(':', missingDelimiterValue = "")
    val source = SOURCES_BY_PREFIX[prefix] ?: return null
    if (!LOCAL_ID.matches(localId)) return null
    return StopId(source, localId)
}
