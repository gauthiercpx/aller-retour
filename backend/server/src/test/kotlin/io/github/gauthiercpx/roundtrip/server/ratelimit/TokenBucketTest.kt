package io.github.gauthiercpx.roundtrip.server.ratelimit

import io.github.gauthiercpx.roundtrip.server.MutableClock
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenBucketTest {
    private val clock = MutableClock(Instant.parse("2026-10-08T12:00:00Z"))
    private val bucket = TokenBucket(clock, capacity = 2, refillInterval = Duration.ofSeconds(90))

    @Test
    fun allowsABurstUpToCapacity() {
        assertTrue(bucket.tryAcquire())
        assertTrue(bucket.tryAcquire())
    }

    @Test
    fun deniesOnceCapacityIsSpent() {
        bucket.tryAcquire()
        bucket.tryAcquire()

        assertFalse(bucket.tryAcquire())
    }

    @Test
    fun deniesBeforeAFullIntervalHasPassed() {
        bucket.tryAcquire()
        bucket.tryAcquire()
        clock.advanceBy(Duration.ofSeconds(89))

        assertFalse(bucket.tryAcquire())
    }

    @Test
    fun refillsOneTokenPerInterval() {
        bucket.tryAcquire()
        bucket.tryAcquire()
        clock.advanceBy(Duration.ofSeconds(90))

        assertTrue(bucket.tryAcquire())
        assertFalse(bucket.tryAcquire())
    }

    @Test
    fun doesNotAccumulateBeyondCapacityWhileIdle() {
        clock.advanceBy(Duration.ofHours(1))

        val granted = listOf(bucket.tryAcquire(), bucket.tryAcquire(), bucket.tryAcquire())

        assertEquals(listOf(true, true, false), granted)
    }

    @Test
    fun startsTheRefillIntervalWhenTheFirstTokenIsTakenAfterIdling() {
        clock.advanceBy(Duration.ofSeconds(89))
        bucket.tryAcquire()
        clock.advanceBy(Duration.ofSeconds(1))

        val granted = listOf(bucket.tryAcquire(), bucket.tryAcquire())

        assertEquals(listOf(true, false), granted)
    }

    @Test
    fun rejectsSubMillisecondRefillInterval() {
        assertFailsWith<IllegalArgumentException> { TokenBucket(clock, capacity = 1, refillInterval = Duration.ofNanos(1)) }
    }

    @Test
    fun rejectsNonPositiveCapacity() {
        assertFailsWith<IllegalArgumentException> { TokenBucket(clock, capacity = 0, refillInterval = Duration.ofSeconds(1)) }
    }
}
