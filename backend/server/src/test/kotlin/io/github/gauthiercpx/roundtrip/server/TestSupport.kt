package io.github.gauthiercpx.roundtrip.server

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Raw upstream samples from docs/api-samples/, put on the test classpath by server/build.gradle.kts. */
object Fixtures {
    const val PRIM_MAGENTA = "prim-stop-monitoring-magenta-rer-e-20261008.json"
    const val STAR_METRO_GROS_CHENE = "star-metro-passages-groschene-20261008.json"
    const val STAR_BUS_PASSAGES = "star-bus-passages-20261008.json"

    /** Shortly before the first departure in the PRIM and metro samples (captured 2026-10-08 ~11:54Z). */
    val SAMPLE_TIME: Instant = Instant.parse("2026-10-08T11:54:00Z")

    fun read(name: String): String =
        requireNotNull(Fixtures::class.java.getResource("/$name")) { "Missing fixture $name" }.readText()
}

class MutableClock(private var now: Instant) : Clock() {
    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    fun advanceBy(duration: Duration) {
        now = now.plus(duration)
    }
}
