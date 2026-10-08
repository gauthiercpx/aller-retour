package io.github.gauthiercpx.roundtrip.data

import io.github.gauthiercpx.roundtrip.datastore.DeparturesCacheStore
import io.github.gauthiercpx.roundtrip.datastore.SettingsStore
import io.github.gauthiercpx.roundtrip.datastore.UserSettings
import io.github.gauthiercpx.roundtrip.domain.StopPlan
import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import io.github.gauthiercpx.roundtrip.network.BackendException
import io.github.gauthiercpx.roundtrip.network.DeparturesRemoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeparturesRepositoryTest {
    private class FakeSettingsStore(initial: UserSettings) : SettingsStore {
        private val state = MutableStateFlow(initial)
        override val settings: Flow<UserSettings> = state

        override suspend fun save(settings: UserSettings) {
            state.value = settings
        }
    }

    private class FakeCacheStore : DeparturesCacheStore {
        val state = MutableStateFlow<DeparturesResponse?>(null)
        override val cached: Flow<DeparturesResponse?> = state

        override suspend fun save(response: DeparturesResponse) {
            state.value = response
        }
    }

    private class FakeRemote(var answer: () -> DeparturesResponse) : DeparturesRemoteSource {
        val requests = mutableListOf<Pair<String, List<String>>>()

        override suspend fun fetch(baseUrl: String, stops: List<String>): DeparturesResponse {
            requests += baseUrl to stops
            return answer()
        }
    }

    private val response = DeparturesResponse(Instant.parse("2026-10-08T08:00:00Z"), emptyList())
    private val configured = UserSettings(
        baseUrl = "https://rt.example",
        plan = StopPlan(morning = listOf("idfm:1"), evening = listOf("star-metro:2")),
    )

    private fun clockAt(isoUtc: String) = Clock.fixed(Instant.parse(isoUtc), ZoneOffset.UTC)

    private fun repository(
        settings: UserSettings = configured,
        cache: FakeCacheStore = FakeCacheStore(),
        remote: FakeRemote = FakeRemote { response },
        now: String = "2026-10-08T08:00:00Z",
    ) = DeparturesRepository(FakeSettingsStore(settings), cache, remote, clockAt(now))

    @Test
    fun refreshStoresTheFetchedResponse() = runTest {
        val cache = FakeCacheStore()

        val result = repository(cache = cache).refresh()

        assertEquals(RefreshResult.Success, result)
        assertEquals(response, cache.cached.first())
    }

    @Test
    fun morningRefreshQueriesTheMorningStopsWithANormalizedUrl() = runTest {
        val remote = FakeRemote { response }

        repository(remote = remote, now = "2026-10-08T07:59:00Z").refresh()

        assertEquals(listOf("https://rt.example/" to listOf("idfm:1")), remote.requests)
    }

    @Test
    fun eveningRefreshQueriesTheEveningStops() = runTest {
        val remote = FakeRemote { response }

        repository(remote = remote, now = "2026-10-08T18:00:00Z").refresh()

        assertEquals(listOf("star-metro:2"), remote.requests.single().second)
    }

    @Test
    fun blankBaseUrlIsNotConfiguredAndMakesNoRequest() = runTest {
        val remote = FakeRemote { response }

        val result = repository(settings = configured.copy(baseUrl = ""), remote = remote).refresh()

        assertEquals(RefreshResult.NotConfigured, result)
        assertEquals(emptyList(), remote.requests)
    }

    @Test
    fun cleartextBaseUrlIsNotConfigured() = runTest {
        val result = repository(settings = configured.copy(baseUrl = "http://rt.example")).refresh()

        assertEquals(RefreshResult.NotConfigured, result)
    }

    @Test
    fun noStopsForTheCurrentSlotIsNotConfigured() = runTest {
        val settings = configured.copy(plan = StopPlan(morning = listOf("idfm:1")))

        val result = repository(settings = settings, now = "2026-10-08T18:00:00Z").refresh()

        assertEquals(RefreshResult.NotConfigured, result)
    }

    @Test
    fun failedFetchReportsTheErrorAndKeepsThePreviousSnapshot() = runTest {
        val cache = FakeCacheStore().apply { state.value = response }
        val remote = FakeRemote { throw BackendException("Backend answered HTTP 503") }

        val result = repository(cache = cache, remote = remote).refresh()

        assertEquals(RefreshResult.Failure("Backend answered HTTP 503"), result)
        assertEquals(response, cache.cached.first())
    }

    @Test
    fun snapshotIsNullBeforeTheFirstRefresh() = runTest {
        assertNull(repository().snapshot.first())
    }
}
