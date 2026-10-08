package io.github.gauthiercpx.roundtrip.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.gauthiercpx.roundtrip.domain.StopPlan
import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import io.github.gauthiercpx.roundtrip.model.StopResult
import io.github.gauthiercpx.roundtrip.model.StopState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataStoreStoresTest {
    private fun TestScope.newDataStore(): DataStore<Preferences> {
        val file = Files.createTempDirectory("datastore-test").resolve("test.preferences_pb").toFile()
        return PreferenceDataStoreFactory.create(scope = backgroundScope as CoroutineScope) { file }
    }

    @Test
    fun settingsDefaultToBlankUrlAndEmptyPlan() = runTest {
        val store = DataStoreSettingsStore(newDataStore())

        assertEquals(UserSettings(), store.settings.first())
    }

    @Test
    fun settingsRoundTrip() = runTest {
        val store = DataStoreSettingsStore(newDataStore())
        val settings = UserSettings(
            baseUrl = "https://rt.example/",
            apiToken = "tok+en/=",
            plan = StopPlan(morning = listOf("idfm:1", "idfm:2"), evening = listOf("star-metro:3")),
        )

        store.save(settings)

        assertEquals(settings, store.settings.first())
    }

    @Test
    fun emptyStopListsStayEmpty() = runTest {
        val store = DataStoreSettingsStore(newDataStore())

        store.save(UserSettings(baseUrl = "https://rt.example/"))

        assertEquals(StopPlan(), store.settings.first().plan)
    }

    @Test
    fun cacheIsEmptyUntilSaved() = runTest {
        val store = DataStoreDeparturesCacheStore(newDataStore())

        assertNull(store.cached.first())
    }

    @Test
    fun cacheRoundTrip() = runTest {
        val store = DataStoreDeparturesCacheStore(newDataStore())
        val response = DeparturesResponse(
            generatedAt = Instant.parse("2026-10-08T08:00:00Z"),
            stops = listOf(StopResult("idfm:1", StopState.STALE, fetchedAt = null, departures = emptyList())),
        )

        store.save(response)

        assertEquals(response, store.cached.first())
    }

    @Test
    fun unreadableCachedPayloadIsTreatedAsEmpty() = runTest {
        val dataStore = newDataStore()
        dataStore.edit { it[stringPreferencesKey("cached_departures")] = "{broken" }

        assertNull(DataStoreDeparturesCacheStore(dataStore).cached.first())
    }
}
