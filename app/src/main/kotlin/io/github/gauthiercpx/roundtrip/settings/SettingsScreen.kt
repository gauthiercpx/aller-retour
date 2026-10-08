package io.github.gauthiercpx.roundtrip.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gauthiercpx.roundtrip.R

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBaseUrlChange = viewModel::onBaseUrlChange,
        onMorningStopsChange = viewModel::onMorningStopsChange,
        onEveningStopsChange = viewModel::onEveningStopsChange,
        onSaveAndRefresh = viewModel::saveAndRefresh,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBaseUrlChange: (String) -> Unit,
    onMorningStopsChange: (String) -> Unit,
    onEveningStopsChange: (String) -> Unit,
    onSaveAndRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Text(
            text = stringResource(R.string.settings_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingsField(
            label = stringResource(R.string.settings_base_url),
            hint = stringResource(R.string.settings_base_url_hint),
            value = state.baseUrl,
            error = if (state.isBaseUrlInvalid) stringResource(R.string.settings_error_base_url) else null,
            onValueChange = onBaseUrlChange,
        )
        SettingsField(
            label = stringResource(R.string.settings_morning_stops),
            hint = stringResource(R.string.settings_stops_hint),
            value = state.morningStops,
            error = state.morningStopsError,
            onValueChange = onMorningStopsChange,
        )
        SettingsField(
            label = stringResource(R.string.settings_evening_stops),
            hint = stringResource(R.string.settings_stops_hint),
            value = state.eveningStops,
            error = state.eveningStopsError,
            onValueChange = onEveningStopsChange,
        )

        Button(onClick = onSaveAndRefresh, enabled = !state.isBusy) {
            Text(text = stringResource(R.string.settings_save_and_refresh))
        }

        when (val status = state.status) {
            is SettingsStatus.Message -> Text(text = stringResource(status.resId))

            is SettingsStatus.RefreshFailed ->
                Text(
                    text = stringResource(R.string.settings_status_failed, status.reason),
                    color = MaterialTheme.colorScheme.error,
                )

            null -> Unit
        }
    }
}

@Composable
private fun SettingsField(
    label: String,
    hint: String,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(hint) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
}
