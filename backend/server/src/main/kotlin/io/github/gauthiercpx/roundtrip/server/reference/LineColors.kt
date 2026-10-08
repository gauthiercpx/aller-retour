package io.github.gauthiercpx.roundtrip.server.reference

import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network

/** What identifies a line across the upstreams: the PRIM `LineRef` for IDFM, STAR's `idligne` for Rennes. */
data class LineKey(val network: Network, val mode: Mode, val lineId: String)

/** Official line colour as `#rrggbb`. A lookup that cannot answer returns null and never throws. */
fun interface LineColorSource {
    suspend fun color(key: LineKey): String?
}

object NoLineColors : LineColorSource {
    override suspend fun color(key: LineKey): String? = null
}
