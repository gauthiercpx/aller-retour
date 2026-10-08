package io.github.gauthiercpx.roundtrip.server.ratelimit

import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Token bucket: up to [capacity] calls in a burst, then one more per [refillInterval].
 *
 * State is in memory, so a restart refills the bucket. Acceptable for a single replica.
 */
class TokenBucket(
    private val clock: Clock,
    private val capacity: Int,
    private val refillInterval: Duration,
) {
    init {
        require(capacity > 0) { "capacity must be positive, got $capacity" }
        require(refillInterval.toMillis() > 0) { "refillInterval must be at least 1 ms, got $refillInterval" }
    }

    private var available = capacity
    private var lastRefill: Instant = clock.instant()

    /** Takes a token if one is available. Never blocks. */
    @Synchronized
    fun tryAcquire(): Boolean {
        refill()
        if (available == 0) return false
        available--
        return true
    }

    private fun refill() {
        val now = clock.instant()
        // A full bucket earns nothing, so its refill interval only starts once a token is taken.
        if (available == capacity) {
            lastRefill = now
            return
        }
        val earned = Duration.between(lastRefill, now).toMillis() / refillInterval.toMillis()
        if (earned <= 0) return

        available = minOf(capacity.toLong(), available + earned).toInt()
        lastRefill = if (available == capacity) now else lastRefill.plus(refillInterval.multipliedBy(earned))
    }
}
