package io.github.gauthiercpx.roundtrip.network

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.model.StopResult
import io.github.gauthiercpx.roundtrip.model.StopState
import io.github.gauthiercpx.roundtrip.model.Vehicle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RetrofitDeparturesSourceTest {
    private val server = MockWebServer().apply { start() }
    private val json = Json { ignoreUnknownKeys = true }
    private val source = RetrofitDeparturesSource(OkHttpClient(), json)
    private val baseUrl = server.url("/").toString()

    private val response = DeparturesResponse(
        generatedAt = Instant.parse("2026-10-08T08:00:00Z"),
        stops = listOf(
            StopResult(
                stopId = "idfm:58572",
                state = StopState.FRESH,
                fetchedAt = Instant.parse("2026-10-08T08:00:00Z"),
                departures = listOf(
                    Departure(
                        network = Network.IDFM,
                        stopId = "idfm:58572",
                        stopName = "Magenta",
                        lineId = "L",
                        lineName = "E",
                        lineColor = null,
                        mode = Mode.RER,
                        direction = "Aller",
                        destination = "Tournan",
                        scheduledTime = null,
                        expectedTime = Instant.parse("2026-10-08T08:05:00Z"),
                        delaySeconds = null,
                        status = DepartureStatus.ON_TIME,
                        isRealtime = true,
                        vehicle = Vehicle.UNKNOWN,
                        missionCode = null,
                    ),
                ),
            ),
        ),
    )

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun decodesTheBackendResponse() = runTest {
        server.enqueue(MockResponse().setBody(Json.encodeToString(DeparturesResponse.serializer(), response)))

        val result = source.fetch(baseUrl, "", listOf("idfm:58572"))

        assertEquals(response, result)
    }

    @Test
    fun sendsStopsAsOneCommaSeparatedQueryParameter() = runTest {
        server.enqueue(MockResponse().setBody(Json.encodeToString(DeparturesResponse.serializer(), response)))

        source.fetch(baseUrl, "", listOf("idfm:58572", "star-metro:5074"))

        val request = server.takeRequest()
        assertEquals("/departures", request.requestUrl?.encodedPath)
        assertEquals("idfm:58572,star-metro:5074", request.requestUrl?.queryParameter("stops"))
    }

    @Test
    fun ignoresUnknownFieldsInTheResponse() = runTest {
        val body = Json.encodeToString(DeparturesResponse.serializer(), response)
            .replaceFirst("{", """{"futureField":1,""")
        server.enqueue(MockResponse().setBody(body))

        assertEquals(response, source.fetch(baseUrl, "", listOf("idfm:58572")))
    }

    @Test
    fun reportsTheStatusCodeOnHttpErrors() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val error = assertFailsWith<BackendException> { source.fetch(baseUrl, "", listOf("idfm:58572")) }

        assertTrue("503" in error.message.orEmpty())
    }

    @Test
    fun failsWithBackendExceptionOnMalformedJson() = runTest {
        server.enqueue(MockResponse().setBody("not json"))

        assertFailsWith<BackendException> { source.fetch(baseUrl, "", listOf("idfm:58572")) }
    }

    @Test
    fun failsWithBackendExceptionWhenTheServerIsUnreachable() = runTest {
        server.shutdown()

        assertFailsWith<BackendException> { source.fetch(baseUrl, "", listOf("idfm:58572")) }
    }

    @Test
    fun failsWithBackendExceptionOnAnInvalidBaseUrl() = runTest {
        assertFailsWith<BackendException> { source.fetch("not a url", "", listOf("idfm:58572")) }
    }

    @Test
    fun sendsTheTokenAsABearerHeader() = runTest {
        server.enqueue(MockResponse().setBody(Json.encodeToString(DeparturesResponse.serializer(), response)))

        source.fetch(baseUrl, "tok+en/=", listOf("idfm:58572"))

        assertEquals("Bearer tok+en/=", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun sendsNoAuthorizationHeaderWhenTheTokenIsBlank() = runTest {
        server.enqueue(MockResponse().setBody(Json.encodeToString(DeparturesResponse.serializer(), response)))

        source.fetch(baseUrl, "  ", listOf("idfm:58572"))

        assertEquals(null, server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun explainsA401AsARefusedToken() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val error = assertFailsWith<BackendException> { source.fetch(baseUrl, "wrong", listOf("idfm:58572")) }

        assertTrue("token" in error.message.orEmpty())
    }
}
