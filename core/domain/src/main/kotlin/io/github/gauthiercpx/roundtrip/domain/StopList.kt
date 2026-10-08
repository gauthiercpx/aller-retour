package io.github.gauthiercpx.roundtrip.domain

/** Mirrors the backend limit on `stops` per request. */
const val MAX_STOPS_PER_REQUEST = 10

private val STOP_ID = Regex("""(idfm|star-metro|star-bus):\d{1,9}""")

sealed interface StopListParse {
    data class Valid(val stops: List<String>) : StopListParse
    data class Invalid(val message: String) : StopListParse
}

/** Parses a comma-separated list of backend stop ids, keeping order and dropping duplicates and blanks. */
fun parseStopList(raw: String): StopListParse {
    val tokens = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val invalidToken = tokens.firstOrNull { !STOP_ID.matches(it) }
    val stops = tokens.distinct()
    return when {
        invalidToken != null -> StopListParse.Invalid("'$invalidToken' is not a stop id, expected e.g. idfm:58572")

        stops.size > MAX_STOPS_PER_REQUEST ->
            StopListParse.Invalid("At most $MAX_STOPS_PER_REQUEST stops, got ${stops.size}")

        else -> StopListParse.Valid(stops)
    }
}
