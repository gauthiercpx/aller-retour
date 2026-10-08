package io.github.gauthiercpx.roundtrip.server

import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import io.github.gauthiercpx.roundtrip.model.StopState
import io.github.gauthiercpx.roundtrip.server.cache.TtlCache
import io.github.gauthiercpx.roundtrip.server.ratelimit.TokenBucket
import io.github.gauthiercpx.roundtrip.server.reference.NoLineColors
import io.github.gauthiercpx.roundtrip.server.upstream.prim.PrimClient
import io.github.gauthiercpx.roundtrip.server.upstream.star.StarClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutesTest {
    private val clock = MutableClock(Fixtures.SAMPLE_TIME)
    private val config = AppConfig(primApiKey = "dummy-test-key", apiToken = TEST_TOKEN, port = 0)

    /** Stands in for PRIM and STAR, answering with the saved samples. Never touches the network. */
    private class FakeUpstreams {
        var primStatus = HttpStatusCode.OK
        val requests = CopyOnWriteArrayList<HttpRequestData>()

        val client = HttpClient(MockEngine { request ->
            requests += request
            val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
            when {
                request.url.host == PRIM_HOST && primStatus == HttpStatusCode.OK ->
                    respond(Fixtures.read(Fixtures.PRIM_MAGENTA), HttpStatusCode.OK, jsonHeaders)
                request.url.host == PRIM_HOST ->
                    respond("upstream error", primStatus)
                request.url.encodedPath.contains(StarClient.METRO_DATASET) ->
                    respond(Fixtures.read(Fixtures.STAR_METRO_GROS_CHENE), HttpStatusCode.OK, jsonHeaders)
                request.url.encodedPath.contains(StarClient.BUS_DATASET) ->
                    respond(Fixtures.read(Fixtures.STAR_BUS_PASSAGES), HttpStatusCode.OK, jsonHeaders)
                else -> error("Unexpected upstream request ${request.url}")
            }
        })

        fun primRequests(): List<HttpRequestData> = requests.filter { it.url.host == PRIM_HOST }

        companion object {
            const val PRIM_HOST = "prim.iledefrance-mobilites.fr"
        }
    }

    private val fake = FakeUpstreams()

    private fun ApplicationTestBuilder.startApp(
        appConfig: AppConfig = config,
        service: DepartureService = DepartureService.create(
            PrimClient(fake.client, appConfig.primApiKey),
            StarClient(fake.client),
            NoLineColors,
            clock,
        ),
    ) {
        application { roundTripModule(appConfig, service) }
    }

    /** A client that sends the bearer token, as the app does. */
    private fun ApplicationTestBuilder.authed() = createClient {
        defaultRequest { header(HttpHeaders.Authorization, "Bearer $TEST_TOKEN") }
    }

    private suspend fun HttpResponse.departures(): DeparturesResponse =
        Json.decodeFromString(DeparturesResponse.serializer(), bodyAsText())

    @Test
    fun healthzReturnsOk() = testApplication {
        startApp()

        val response = client.get("/healthz")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.bodyAsText())
    }

    @Test
    fun readyzReturnsOkWhenPrimKeyIsSet() = testApplication {
        startApp()

        val response = client.get("/readyz")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(Readiness(emptyList()), Json.decodeFromString(Readiness.serializer(), response.bodyAsText()))
    }

    @Test
    fun readyzReturns503NamingTheMissingKey() = testApplication {
        startApp(appConfig = AppConfig(primApiKey = null, apiToken = TEST_TOKEN, port = 0))

        val response = client.get("/readyz")

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals(Readiness(listOf("PRIM_API_KEY")), Json.decodeFromString(Readiness.serializer(), response.bodyAsText()))
    }

    @Test
    fun departuresReturnsOneFreshResultPerStopInRequestOrder() = testApplication {
        startApp()

        val body = authed().get("/departures?stops=star-metro:5074,idfm:58572").departures()

        assertEquals(listOf("star-metro:5074", "idfm:58572"), body.stops.map { it.stopId })
        assertEquals(listOf(StopState.FRESH, StopState.FRESH), body.stops.map { it.state })
    }

    @Test
    fun departuresAreSortedAndCappedPerStop() = testApplication {
        startApp()

        val prim = authed().get("/departures?stops=idfm:58572").departures().stops.single()

        // The Magenta sample has 65 visits; the earliest leaves at 11:56:23.
        assertEquals(DepartureService.MAX_DEPARTURES_PER_STOP, prim.departures.size)
        assertEquals(Instant.parse("2026-10-08T11:56:23Z"), prim.departures.first().expectedTime)
    }

    @Test
    fun primCallCarriesApiKeyHeaderAndStopAreaReference() = testApplication {
        startApp()

        authed().get("/departures?stops=idfm:58572")

        val request = fake.primRequests().single()
        assertEquals("dummy-test-key", request.headers["apikey"])
        assertEquals("STIF:StopArea:SP:58572:", request.url.parameters["MonitoringRef"])
    }

    @Test
    fun starQueryFiltersOnTheValidatedStopId() = testApplication {
        startApp()

        authed().get("/departures?stops=star-metro:5074")

        val parameters = fake.requests.single().url.parameters
        assertEquals("idarret=\"5074\"", parameters["where"])
        assertEquals("depart", parameters["order_by"])
        assertEquals("20", parameters["limit"])
    }

    @Test
    fun invalidStopsReturn400WithoutCallingUpstreams() = testApplication {
        startApp()

        val response = authed().get("/departures?stops=sncf:87271007")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(fake.requests.isEmpty())
    }

    @Test
    fun repeatedRequestWithinTtlIsServedFromCache() = testApplication {
        startApp()

        authed().get("/departures?stops=idfm:58572")
        clock.advanceBy(Duration.ofSeconds(59))
        val body = authed().get("/departures?stops=idfm:58572").departures()

        assertEquals(StopState.FRESH, body.stops.single().state)
        assertEquals(1, fake.primRequests().size)
    }

    @Test
    fun upstreamErrorWithWarmCacheServesStaleDepartures() = testApplication {
        startApp()
        authed().get("/departures?stops=idfm:58572")
        clock.advanceBy(Duration.ofSeconds(61))
        fake.primStatus = HttpStatusCode.InternalServerError

        val result = authed().get("/departures?stops=idfm:58572").departures().stops.single()

        assertEquals(StopState.STALE, result.state)
        assertEquals(Fixtures.SAMPLE_TIME, result.fetchedAt)
        assertEquals(DepartureService.MAX_DEPARTURES_PER_STOP, result.departures.size)
    }

    @Test
    fun upstreamErrorWithColdCacheLeavesOtherStopsUnaffected() = testApplication {
        startApp()
        fake.primStatus = HttpStatusCode.InternalServerError

        val body = authed().get("/departures?stops=idfm:58572,star-metro:5074").departures()

        assertEquals(listOf(StopState.UNAVAILABLE, StopState.FRESH), body.stops.map { it.state })
        assertEquals(emptyList(), body.stops.first().departures)
    }

    @Test
    fun missingPrimKeyMakesIdfmStopsUnavailableWithoutCallingPrim() = testApplication {
        startApp(appConfig = AppConfig(primApiKey = null, apiToken = TEST_TOKEN, port = 0))

        val result = authed().get("/departures?stops=idfm:58572").departures().stops.single()

        assertEquals(StopState.UNAVAILABLE, result.state)
        assertTrue(fake.primRequests().isEmpty())
    }

    @Test
    fun rateLimitedStopIsServedFromCacheWithoutCallingUpstream() = testApplication {
        val prim = PrimClient(fake.client, config.primApiKey)
        startApp(
            service = DepartureService(
                upstreams = mapOf(
                    StopSource.IDFM to GuardedUpstream(
                        name = "prim",
                        limiter = TokenBucket(clock, capacity = 1, refillInterval = Duration.ofHours(1)),
                        cache = TtlCache(
                            clock,
                            ttl = Duration.ofSeconds(60),
                            staleRetention = Duration.ofMinutes(10),
                            failureBackoff = Duration.ofSeconds(30),
                        ),
                        fetch = prim::departures,
                    ),
                ),
                clock = clock,
                lineColors = NoLineColors,
            ),
        )
        authed().get("/departures?stops=idfm:58572")
        clock.advanceBy(Duration.ofSeconds(61))

        val result = authed().get("/departures?stops=idfm:58572").departures().stops.single()

        assertEquals(StopState.STALE, result.state)
        assertEquals(1, fake.primRequests().size)
    }

    @Test
    fun departuresWithoutATokenReturn401AndNeverCallUpstreams() = testApplication {
        startApp()

        val response = client.get("/departures?stops=idfm:58572")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("Bearer", response.headers[HttpHeaders.WWWAuthenticate])
        assertTrue(fake.requests.isEmpty())
    }

    @Test
    fun departuresWithAWrongTokenReturn401() = testApplication {
        startApp()

        val response = client.get("/departures?stops=idfm:58572") {
            header(HttpHeaders.Authorization, "Bearer not-the-token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun departuresAreRefusedWhenTheServerHasNoTokenConfigured() = testApplication {
        startApp(appConfig = AppConfig(primApiKey = "dummy-test-key", apiToken = null, port = 0))

        val response = client.get("/departures?stops=idfm:58572") {
            header(HttpHeaders.Authorization, "Bearer $TEST_TOKEN")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun probeEndpointsStayOpenWithoutAToken() = testApplication {
        startApp()

        assertEquals(HttpStatusCode.OK, client.get("/healthz").status)
        assertEquals(HttpStatusCode.OK, client.get("/readyz").status)
    }

    @Test
    fun readyzNamesTheMissingApiToken() = testApplication {
        startApp(appConfig = AppConfig(primApiKey = "dummy-test-key", apiToken = null, port = 0))

        val response = client.get("/readyz")

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue("API_TOKEN" in response.bodyAsText())
    }

    private companion object {
        const val TEST_TOKEN = "dummy-test-token"
    }
}
