package io.github.gauthiercpx.roundtrip.datastore

import io.github.gauthiercpx.roundtrip.domain.StopPlan
import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import kotlinx.coroutines.flow.Flow

/** [baseUrl] stays blank until the user sets it; the app never ships a backend address. */
data class UserSettings(val baseUrl: String = "", val plan: StopPlan = StopPlan())

interface SettingsStore {
    val settings: Flow<UserSettings>

    suspend fun save(settings: UserSettings)
}

/** Last backend answer, kept so the widget has something to show when the phone is offline. */
interface DeparturesCacheStore {
    /** Null when nothing is cached yet or the cached payload can no longer be read. */
    val cached: Flow<DeparturesResponse?>

    suspend fun save(response: DeparturesResponse)
}
