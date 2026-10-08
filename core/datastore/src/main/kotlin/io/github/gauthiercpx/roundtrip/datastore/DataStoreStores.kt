package io.github.gauthiercpx.roundtrip.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.gauthiercpx.roundtrip.domain.StopPlan
import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject

private val BASE_URL = stringPreferencesKey("base_url")
private val MORNING_STOPS = stringPreferencesKey("morning_stops")
private val EVENING_STOPS = stringPreferencesKey("evening_stops")
private val CACHED_DEPARTURES = stringPreferencesKey("cached_departures")

class DataStoreSettingsStore @Inject constructor(private val dataStore: DataStore<Preferences>) : SettingsStore {
    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            baseUrl = prefs[BASE_URL].orEmpty(),
            plan = StopPlan(
                morning = prefs[MORNING_STOPS].toStopList(),
                evening = prefs[EVENING_STOPS].toStopList(),
            ),
        )
    }

    override suspend fun save(settings: UserSettings) {
        dataStore.edit { prefs ->
            prefs[BASE_URL] = settings.baseUrl
            prefs[MORNING_STOPS] = settings.plan.morning.joinToString(",")
            prefs[EVENING_STOPS] = settings.plan.evening.joinToString(",")
        }
    }
}

class DataStoreDeparturesCacheStore @Inject constructor(private val dataStore: DataStore<Preferences>) :
    DeparturesCacheStore {
    private val json = Json { ignoreUnknownKeys = true }

    override val cached: Flow<DeparturesResponse?> = dataStore.data.map { prefs ->
        prefs[CACHED_DEPARTURES]?.let(::decodeOrNull)
    }

    override suspend fun save(response: DeparturesResponse) {
        dataStore.edit { prefs ->
            prefs[CACHED_DEPARTURES] = json.encodeToString(DeparturesResponse.serializer(), response)
        }
    }

    // A payload written by an older app version may no longer decode; a refresh replaces it, so treat it as empty.
    private fun decodeOrNull(raw: String): DeparturesResponse? = try {
        json.decodeFromString(DeparturesResponse.serializer(), raw)
    } catch (_: SerializationException) {
        null
    }
}

private fun String?.toStopList(): List<String> = this.orEmpty().split(',').filter { it.isNotBlank() }
