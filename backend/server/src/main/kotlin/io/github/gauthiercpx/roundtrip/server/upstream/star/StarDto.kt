package io.github.gauthiercpx.roundtrip.server.upstream.star

import kotlinx.serialization.Serializable

// Opendatasoft Explore v2.1 records responses. Only the mapped fields are declared, all nullable:
// see docs/api-notes/star-*.md for what the real responses contain.

@Serializable
internal data class ExploreRecords<T>(
    val results: List<T>? = null,
)

/** Row of `tco-metro-circulation-passages-tr`. `precision` is an array here. */
@Serializable
internal data class MetroPassage(
    val idligne: String? = null,
    val nomcourtligne: String? = null,
    val sens: Int? = null,
    val destination: String? = null,
    val idarret: String? = null,
    val nomarret: String? = null,
    val depart: String? = null,
    val precision: List<String>? = null,
)

/** Row of `tco-bus-circulation-passages-tr`. `precision` is a plain string here, unlike the metro dataset. */
@Serializable
internal data class BusPassage(
    val idligne: String? = null,
    val nomcourtligne: String? = null,
    val sens: Int? = null,
    val destination: String? = null,
    val idarret: String? = null,
    val nomarret: String? = null,
    val departtheorique: String? = null,
    val depart: String? = null,
    val precision: String? = null,
)
