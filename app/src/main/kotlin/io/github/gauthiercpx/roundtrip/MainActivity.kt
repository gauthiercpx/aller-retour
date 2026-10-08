package io.github.gauthiercpx.roundtrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.gauthiercpx.roundtrip.ui.theme.RoundTripTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoundTripTheme {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.CenterStart) {
                    Text(text = getString(R.string.app_name), style = MaterialTheme.typography.displaySmall)
                }
            }
        }
    }
}
