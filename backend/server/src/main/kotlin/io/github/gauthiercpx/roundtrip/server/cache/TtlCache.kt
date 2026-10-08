package io.github.gauthiercpx.roundtrip.server.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

sealed interface CacheResult<out V> {
    data class Fresh<V>(val value: V, val fetchedAt: Instant) : CacheResult<V>

    /** The load failed; [value] is the last good value, still within the retention window. */
    data class Stale<V>(val value: V, val fetchedAt: Instant, val cause: Exception) : CacheResult<V>

    /** The load failed and nothing usable is cached. */
    data class Missing(val cause: Exception) : CacheResult<Nothing>
}

/**
 * In-memory cache with a freshness TTL and a longer stale-retention window used only when a reload fails.
 *
 * Loads for the same key are serialised. Concurrent misses trigger a single upstream call whether it
 * succeeds or fails: after a failure, further loads are skipped for [failureBackoff] and callers get the
 * stale value (or nothing) with the original cause, so a slow or broken upstream is not hit once per
 * waiting request. Keys are the stops actually requested, which stays small for a single-user service,
 * so entries are not evicted.
 */
class TtlCache<K : Any, V : Any>(
    private val clock: Clock,
    private val ttl: Duration,
    private val staleRetention: Duration,
    private val failureBackoff: Duration,
) {
    private class Entry<V>(val value: V, val fetchedAt: Instant)

    private class Failure(val at: Instant, val cause: Exception)

    private class Slot<V> {
        val lock = Mutex()

        @Volatile
        var entry: Entry<V>? = null

        @Volatile
        var lastFailure: Failure? = null
    }

    private val slots = ConcurrentHashMap<K, Slot<V>>()

    suspend fun get(key: K, load: suspend () -> V): CacheResult<V> {
        val slot = slots.computeIfAbsent(key) { Slot() }
        freshResult(slot)?.let { return it }

        return slot.lock.withLock {
            freshResult(slot) ?: recentFailureResult(slot) ?: reload(slot, load)
        }
    }

    private fun freshResult(slot: Slot<V>): CacheResult.Fresh<V>? {
        val entry = slot.entry ?: return null
        if (age(entry.fetchedAt) >= ttl) return null
        return CacheResult.Fresh(entry.value, entry.fetchedAt)
    }

    private fun recentFailureResult(slot: Slot<V>): CacheResult<V>? {
        val failure = slot.lastFailure ?: return null
        if (age(failure.at) >= failureBackoff) return null
        return degraded(slot, failure.cause)
    }

    private suspend fun reload(slot: Slot<V>, load: suspend () -> V): CacheResult<V> {
        try {
            val entry = Entry(load(), clock.instant())
            slot.entry = entry
            slot.lastFailure = null
            return CacheResult.Fresh(entry.value, entry.fetchedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Any load failure degrades to the last good value; the cause travels with the result.
            slot.lastFailure = Failure(clock.instant(), e)
            return degraded(slot, e)
        }
    }

    private fun degraded(slot: Slot<V>, cause: Exception): CacheResult<V> {
        val stale = slot.entry?.takeIf { age(it.fetchedAt) < staleRetention }
            ?: return CacheResult.Missing(cause)
        return CacheResult.Stale(stale.value, stale.fetchedAt, cause)
    }

    private fun age(since: Instant): Duration = Duration.between(since, clock.instant())
}
