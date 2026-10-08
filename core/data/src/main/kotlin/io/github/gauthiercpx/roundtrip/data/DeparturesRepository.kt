package io.github.gauthiercpx.roundtrip.data

import io.github.gauthiercpx.roundtrip.datastore.DeparturesCacheStore
import io.github.gauthiercpx.roundtrip.datastore.SettingsStore
import io.github.gauthiercpx.roundtrip.domain.normalizeBackendUrl
import io.github.gauthiercpx.roundtrip.domain.timeSlotOf
import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import io.github.gauthiercpx.roundtrip.network.BackendException
import io.github.gauthiercpx.roundtrip.network.DeparturesRemoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RefreshResult {
    data object Success : RefreshResult

    /** No usable backend URL, or no stops planned for the current time slot. */
    data object NotConfigured : RefreshResult

    data class Failure(val message: String) : RefreshResult
}

@Singleton
class DeparturesRepository @Inject constructor(
    private val settingsStore: SettingsStore,
    private val cacheStore: DeparturesCacheStore,
    private val remote: DeparturesRemoteSource,
    private val clock: Clock,
) {
    /** The last backend answer, or null before the first successful refresh. */
    val snapshot: Flow<DeparturesResponse?> get() = cacheStore.cached

    /** Fetches the stops planned for the current time slot. A failed fetch leaves the cached snapshot untouched. */
    suspend fun refresh(): RefreshResult {
        val settings = settingsStore.settings.first()
        val baseUrl = normalizeBackendUrl(settings.baseUrl)
        val stops = settings.plan.stopsFor(timeSlotOf(LocalTime.now(clock)))
        if (baseUrl == null || stops.isEmpty()) return RefreshResult.NotConfigured

        return try {
            cacheStore.save(remote.fetch(baseUrl, settings.apiToken, stops))
            RefreshResult.Success
        } catch (e: BackendException) {
            RefreshResult.Failure(e.message.orEmpty())
        }
    }
}
