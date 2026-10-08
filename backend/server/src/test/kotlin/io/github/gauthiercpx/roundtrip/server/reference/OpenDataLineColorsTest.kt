package io.github.gauthiercpx.roundtrip.server.reference

import io.github.gauthiercpx.roundtrip.model.Mode
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.server.MutableClock
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenDataLineColorsTest {
    private val clock = MutableClock(Instant.parse("2026-10-08T12:00:00Z"))
    private val requests = CopyOnWriteArrayList<HttpRequestData>()
    private var reply: () -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to """{"results":[]}""" }

    private val http = HttpClient(
        MockEngine { request ->
            requests += request
            val (status, body) = reply()
            if (status == HttpStatusCode.OK) {
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respondError(status)
            }
        },
    )
    private val colors = OpenDataLineColors(http, clock)

    private val rerE = LineKey(Network.IDFM, Mode.RER, "STIF:Line::C01729:")
    private val metroB = LineKey(Network.STAR, Mode.METRO, "1002")

    private fun answer(field: String, value: String) {
        reply = { HttpStatusCode.OK to """{"results":[{"$field":"$value"}]}""" }
    }

    @Test
    fun readsTheIdfmColorAndAddsAHash() = runTest {
        answer("colourweb_hexa", "b94e9a")

        assertEquals("#b94e9a", colors.color(rerE))
    }

    @Test
    fun queriesIdfmByTheLineIdWithoutThePrimPrefix() = runTest {
        answer("colourweb_hexa", "b94e9a")

        colors.color(rerE)

        val url = requests.single().url
        assertEquals("data.iledefrance-mobilites.fr", url.host)
        assertEquals("id_line=\"C01729\"", url.parameters["where"])
    }

    @Test
    fun readsTheStarMetroColorFromTheMetroDataset() = runTest {
        answer("couleurligne", "#00893E")

        assertEquals("#00893e", colors.color(metroB))
        assertTrue("tco-metro-topologie-lignes-td" in requests.single().url.encodedPath)
        assertEquals("id=\"1002\"", requests.single().url.parameters["where"])
    }

    @Test
    fun readsStarBusColorsFromTheBusDataset() = runTest {
        answer("couleurligne", "#a96f23")

        assertEquals("#a96f23", colors.color(LineKey(Network.STAR, Mode.BUS, "0014")))
        assertTrue("tco-bus-topologie-lignes-td" in requests.single().url.encodedPath)
    }

    @Test
    fun anUnknownLineGivesNullAndIsNotAskedAgain() = runTest {
        assertNull(colors.color(rerE))
        assertNull(colors.color(rerE))

        assertEquals(1, requests.size)
    }

    @Test
    fun aKnownColorIsServedFromCacheUntilTheDayIsOver() = runTest {
        answer("colourweb_hexa", "b94e9a")
        colors.color(rerE)

        clock.advanceBy(Duration.ofHours(23))
        colors.color(rerE)
        assertEquals(1, requests.size)

        clock.advanceBy(Duration.ofHours(2))
        colors.color(rerE)
        assertEquals(2, requests.size)
    }

    @Test
    fun anHttpErrorGivesNullInsteadOfThrowing() = runTest {
        reply = { HttpStatusCode.InternalServerError to "" }

        assertNull(colors.color(rerE))
    }

    @Test
    fun aFailedLookupIsRetriedOnlyAfterTheBackoff() = runTest {
        reply = { HttpStatusCode.InternalServerError to "" }
        colors.color(rerE)
        colors.color(rerE)
        assertEquals(1, requests.size)

        clock.advanceBy(Duration.ofMinutes(6))
        answer("colourweb_hexa", "b94e9a")

        assertEquals("#b94e9a", colors.color(rerE))
    }

    @Test
    fun keepsTheLastColorWhenARefreshFails() = runTest {
        answer("colourweb_hexa", "b94e9a")
        colors.color(rerE)
        clock.advanceBy(Duration.ofHours(25))
        reply = { HttpStatusCode.InternalServerError to "" }

        assertEquals("#b94e9a", colors.color(rerE))
    }

    @Test
    fun malformedJsonGivesNull() = runTest {
        reply = { HttpStatusCode.OK to "not json" }

        assertNull(colors.color(rerE))
    }

    @Test
    fun anInvalidHexValueGivesNull() = runTest {
        answer("colourweb_hexa", "zzzzzz")

        assertNull(colors.color(rerE))
    }

    @Test
    fun idsThatCouldInjectIntoTheQueryAreNeverSent() = runTest {
        assertNull(colors.color(LineKey(Network.IDFM, Mode.RER, "STIF:Line::C01729\" or 1=1 --:")))
        assertNull(colors.color(LineKey(Network.STAR, Mode.METRO, "1002\" or \"1\"=\"1")))
        assertNull(colors.color(LineKey(Network.STAR, Mode.BUS, "")))

        assertTrue(requests.isEmpty())
    }

    @Test
    fun unsupportedNetworksAreNotLookedUp() = runTest {
        assertNull(colors.color(LineKey(Network.SNCF, Mode.TRAIN, "1")))

        assertTrue(requests.isEmpty())
    }
}
