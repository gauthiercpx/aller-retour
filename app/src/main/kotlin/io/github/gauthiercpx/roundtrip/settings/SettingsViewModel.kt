package io.github.gauthiercpx.roundtrip.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gauthiercpx.roundtrip.R
import io.github.gauthiercpx.roundtrip.data.DeparturesRepository
import io.github.gauthiercpx.roundtrip.data.RefreshResult
import io.github.gauthiercpx.roundtrip.datastore.SettingsStore
import io.github.gauthiercpx.roundtrip.datastore.UserSettings
import io.github.gauthiercpx.roundtrip.domain.StopListParse
import io.github.gauthiercpx.roundtrip.domain.StopPlan
import io.github.gauthiercpx.roundtrip.domain.normalizeApiToken
import io.github.gauthiercpx.roundtrip.domain.normalizeBackendUrl
import io.github.gauthiercpx.roundtrip.domain.parseStopList
import io.github.gauthiercpx.roundtrip.widget.DeparturesWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val baseUrl: String = "",
    val apiToken: String = "",
    val morningStops: String = "",
    val eveningStops: String = "",
    val isBaseUrlInvalid: Boolean = false,
    val isApiTokenInvalid: Boolean = false,
    val morningStopsError: String? = null,
    val eveningStopsError: String? = null,
    val isBusy: Boolean = false,
    val status: SettingsStatus? = null,
)

/** Resource-backed so the ViewModel stays free of string lookups; the screen resolves it. */
sealed interface SettingsStatus {
    data class Message(@StringRes val resId: Int) : SettingsStatus

    data class RefreshFailed(val reason: String) : SettingsStatus
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val repository: DeparturesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = settingsStore.settings.first()
            mutableState.update {
                it.copy(
                    baseUrl = saved.baseUrl,
                    apiToken = saved.apiToken,
                    morningStops = saved.plan.morning.joinToString(", "),
                    eveningStops = saved.plan.evening.joinToString(", "),
                )
            }
        }
    }

    fun onBaseUrlChange(value: String) = mutableState.update { it.copy(baseUrl = value, isBaseUrlInvalid = false) }

    fun onApiTokenChange(value: String) = mutableState.update { it.copy(apiToken = value, isApiTokenInvalid = false) }

    fun onMorningStopsChange(value: String) =
        mutableState.update { it.copy(morningStops = value, morningStopsError = null) }

    fun onEveningStopsChange(value: String) =
        mutableState.update { it.copy(eveningStops = value, eveningStopsError = null) }

    fun saveAndRefresh() {
        val current = mutableState.value
        val baseUrl = current.baseUrl.takeIf { it.isBlank() } ?: normalizeBackendUrl(current.baseUrl)
        val apiToken = normalizeApiToken(current.apiToken)
        val morning = parseStopList(current.morningStops)
        val evening = parseStopList(current.eveningStops)

        val hasInvalidStops = morning is StopListParse.Invalid || evening is StopListParse.Invalid

        if (baseUrl == null || apiToken == null || hasInvalidStops) {
            mutableState.update {
                it.copy(
                    isBaseUrlInvalid = baseUrl == null,
                    isApiTokenInvalid = apiToken == null,
                    morningStopsError = (morning as? StopListParse.Invalid)?.message,
                    eveningStopsError = (evening as? StopListParse.Invalid)?.message,
                )
            }
            return
        }

        viewModelScope.launch {
            mutableState.update { it.copy(isBusy = true, status = null) }
            val plan = StopPlan(
                morning = (morning as StopListParse.Valid).stops,
                evening = (evening as StopListParse.Valid).stops,
            )
            settingsStore.save(UserSettings(baseUrl = baseUrl, apiToken = apiToken, plan = plan))
            val status = when (val result = repository.refresh()) {
                RefreshResult.Success -> SettingsStatus.Message(R.string.settings_status_refreshed)
                RefreshResult.NotConfigured -> SettingsStatus.Message(R.string.settings_status_saved_not_configured)
                is RefreshResult.Failure -> SettingsStatus.RefreshFailed(result.message)
            }
            DeparturesWidget().updateAll(context)
            mutableState.update { it.copy(isBusy = false, status = status) }
        }
    }
}
