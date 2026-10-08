package io.github.gauthiercpx.roundtrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.gauthiercpx.roundtrip.settings.SettingsRoute
import io.github.gauthiercpx.roundtrip.ui.theme.RoundTripTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoundTripTheme {
                SettingsRoute()
            }
        }
    }
}
