package io.github.gauthiercpx.roundtrip.server.cache

import io.github.gauthiercpx.roundtrip.server.MutableClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TtlCacheTest {
    private val start = Instant.parse("2026-10-08T12:00:00Z")
    private val clock = MutableClock(start)
    private val cache = TtlCache<String, String>(
        clock,
        ttl = Duration.ofSeconds(60),
        staleRetention = Duration.ofMinutes(10),
        failureBackoff = Duration.ofSeconds(30),
    )
    private val failure = IOException("upstream down")

    @Test
    fun returnsCachedValueWithinTtlWithoutReloading() = runTest {
        cache.get("k") { "first" }
        clock.advanceBy(Duration.ofSeconds(59))

        val result = cache.get("k") { "second" }

        assertEquals(CacheResult.Fresh("first", start), result)
    }

    @Test
    fun reloadsOnceTtlHasElapsed() = runTest {
        cache.get("k") { "first" }
        clock.advanceBy(Duration.ofSeconds(60))

        val result = cache.get("k") { "second" }

        assertEquals(CacheResult.Fresh("second", start.plusSeconds(60)), result)
    }

    @Test
    fun servesLastGoodValueAsStaleWhenReloadFails() = runTest {
        cache.get("k") { "first" }
        clock.advanceBy(Duration.ofMinutes(5))

        val result = cache.get("k") { throw failure }

        assertEquals(CacheResult.Stale("first", start, failure), result)
    }

    @Test
    fun returnsMissingWhenFirstLoadFails() = runTest {
        val result = cache.get("k") { throw failure }

        assertEquals(CacheResult.Missing(failure), result)
    }

    @Test
    fun returnsMissingWhenLastGoodValueIsPastRetention() = runTest {
        cache.get("k") { "first" }
        clock.advanceBy(Duration.ofMinutes(10))

        val result = cache.get("k") { throw failure }

        assertIs<CacheResult.Missing>(result)
    }

    @Test
    fun keepsKeysIndependent() = runTest {
        cache.get("a") { "value-a" }

        val result = cache.get("b") { "value-b" }

        assertEquals(CacheResult.Fresh("value-b", start), result)
    }

    @OptIn(ExperimentalCoroutinesApi::class) // runCurrent
    @Test
    fun concurrentMissesForOneKeyShareASingleLoad() = runTest {
        val release = CompletableDeferred<Unit>()
        var loads = 0
        val load: suspend () -> String = {
            loads++
            release.await()
            "value"
        }

        val first = async { cache.get("k", load) }
        val second = async { cache.get("k", load) }
        runCurrent()
        release.complete(Unit)

        assertEquals(CacheResult.Fresh("value", start), first.await())
        assertEquals(CacheResult.Fresh("value", start), second.await())
        assertEquals(1, loads)
    }

    @OptIn(ExperimentalCoroutinesApi::class) // runCurrent
    @Test
    fun concurrentFailingLoadsForOneKeyCallUpstreamOnce() = runTest {
        val release = CompletableDeferred<Unit>()
        var loads = 0
        val load: suspend () -> String = {
            loads++
            release.await()
            throw failure
        }

        val first = async { cache.get("k", load) }
        val second = async { cache.get("k", load) }
        runCurrent()
        release.complete(Unit)

        assertEquals(CacheResult.Missing(failure), first.await())
        assertEquals(CacheResult.Missing(failure), second.await())
        assertEquals(1, loads)
    }

    @Test
    fun skipsReloadWhileFailureBackoffRuns() = runTest {
        cache.get("k") { "first" }
        clock.advanceBy(Duration.ofSeconds(60))
        cache.get("k") { throw failure }
        clock.advanceBy(Duration.ofSeconds(29))
        var loads = 0

        val result = cache.get("k") {
            loads++
            "second"
        }

        assertEquals(CacheResult.Stale("first", start, failure), result)
        assertEquals(0, loads)
    }

    @Test
    fun retriesOnceFailureBackoffHasElapsed() = runTest {
        cache.get("k") { throw failure }
        clock.advanceBy(Duration.ofSeconds(30))

        val result = cache.get("k") { "recovered" }

        assertEquals(CacheResult.Fresh("recovered", start.plusSeconds(30)), result)
    }
}
