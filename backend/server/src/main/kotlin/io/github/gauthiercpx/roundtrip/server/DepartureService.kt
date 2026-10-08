package io.github.gauthiercpx.roundtrip.server

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import io.github.gauthiercpx.roundtrip.model.StopResult
import io.github.gauthiercpx.roundtrip.model.StopState
import io.github.gauthiercpx.roundtrip.server.cache.CacheResult
import io.github.gauthiercpx.roundtrip.server.cache.TtlCache
import io.github.gauthiercpx.roundtrip.server.ratelimit.TokenBucket
import io.github.gauthiercpx.roundtrip.server.upstream.MissingConfigException
import io.github.gauthiercpx.roundtrip.server.upstream.RateLimitedException
import io.github.gauthiercpx.roundtrip.server.upstream.prim.PrimClient
import io.github.gauthiercpx.roundtrip.server.upstream.star.StarClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Clock
import java.time.Duration

/** One upstream behind its cache and rate limiter. A limiter denial is handled like any failed load. */
class GuardedUpstream(
    private val name: String,
    private val limiter: TokenBucket,
    private val cache: TtlCache<StopId, List<Departure>>,
    private val fetch: suspend (StopId) -> List<Departure>,
) {
    suspend fun departures(stop: StopId): CacheResult<List<Departure>> = cache.get(stop) {
        if (!limiter.tryAcquire()) throw RateLimitedException(name)
        fetch(stop)
    }
}

class DepartureService(
    private val upstreams: Map<StopSource, GuardedUpstream>,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(DepartureService::class.java)

    /** Fetches every stop concurrently. A failing stop never fails the others. */
    suspend fun departures(stops: List<StopId>): DeparturesResponse = coroutineScope {
        val results = stops.map { stop -> async { resultFor(stop) } }.awaitAll()
        DeparturesResponse(generatedAt = clock.instant(), stops = results)
    }

    private suspend fun resultFor(stop: StopId): StopResult {
        val upstream = upstreams[stop.source]
            ?: error("No upstream configured for ${stop.source}")

        return when (val result = upstream.departures(stop)) {
            is CacheResult.Fresh ->
                StopResult(stop.toString(), StopState.FRESH, result.fetchedAt, upcoming(result.value))

            is CacheResult.Stale -> {
                logFailure(stop, "serving stale departures", result.cause)
                StopResult(stop.toString(), StopState.STALE, result.fetchedAt, upcoming(result.value))
            }

            is CacheResult.Missing -> {
                logFailure(stop, "no departures available", result.cause)
                StopResult(stop.toString(), StopState.UNAVAILABLE, fetchedAt = null, departures = emptyList())
            }
        }
    }

    /** Drops departures that have already left, so stale cache never shows a gone train. */
    private fun upcoming(departures: List<Departure>): List<Departure> {
        val cutoff = clock.instant().minus(DEPARTED_GRACE)
        return departures
            .filter { !it.expectedTime.isBefore(cutoff) }
            .sortedBy { it.expectedTime }
            .take(MAX_DEPARTURES_PER_STOP)
    }

    // Expected failures (local limiter, missing key, and the IOExceptions: HTTP errors, timeouts, unusable
    // payloads) log one line; anything else, such as a JSON shape change, keeps its stack trace.
    private fun logFailure(stop: StopId, outcome: String, cause: Exception) {
        when (cause) {
            is RateLimitedException -> log.info("{}: {} ({})", stop, outcome, cause.message)
            is MissingConfigException, is IOException -> log.warn("{}: {} ({})", stop, outcome, cause.toString())
            else -> log.warn("{}: {}", stop, outcome, cause)
        }
    }

    companion object {
        const val MAX_DEPARTURES_PER_STOP = 20
        private val DEPARTED_GRACE: Duration = Duration.ofSeconds(60)
        private val STALE_RETENTION: Duration = Duration.ofMinutes(10)
        // After a failed load, skip further upstream calls for this long and serve stale or nothing.
        private val FAILURE_BACKOFF: Duration = Duration.ofSeconds(30)

        // PRIM allows 1000 calls/day per key (x-ratelimit-limit-day, docs/api-notes/prim-stop-monitoring.md).
        // One token per 90 s caps sustained use at 960/day; the 60 s cache absorbs repeated refreshes.
        private val PRIM_TTL: Duration = Duration.ofSeconds(60)
        private const val PRIM_BURST = 10
        private val PRIM_REFILL: Duration = Duration.ofSeconds(90)

        // STAR publishes no rate limits, so stay conservative.
        private val STAR_TTL: Duration = Duration.ofSeconds(30)
        private const val STAR_BURST = 10
        private val STAR_REFILL: Duration = Duration.ofSeconds(2)

        fun create(prim: PrimClient, star: StarClient, clock: Clock): DepartureService {
            val primUpstream = GuardedUpstream(
                name = PrimClient.UPSTREAM_NAME,
                limiter = TokenBucket(clock, PRIM_BURST, PRIM_REFILL),
                cache = TtlCache(clock, PRIM_TTL, STALE_RETENTION, FAILURE_BACKOFF),
                fetch = prim::departures,
            )
            // Metro and bus are two datasets on the same STAR API, so they share one limiter.
            val starLimiter = TokenBucket(clock, STAR_BURST, STAR_REFILL)
            val starMetro = GuardedUpstream(
                StarClient.UPSTREAM_NAME, starLimiter, TtlCache(clock, STAR_TTL, STALE_RETENTION, FAILURE_BACKOFF), star::metroDepartures,
            )
            val starBus = GuardedUpstream(
                StarClient.UPSTREAM_NAME, starLimiter, TtlCache(clock, STAR_TTL, STALE_RETENTION, FAILURE_BACKOFF), star::busDepartures,
            )

            return DepartureService(
                upstreams = mapOf(
                    StopSource.IDFM to primUpstream,
                    StopSource.STAR_METRO to starMetro,
                    StopSource.STAR_BUS to starBus,
                ),
                clock = clock,
            )
        }
    }
}
