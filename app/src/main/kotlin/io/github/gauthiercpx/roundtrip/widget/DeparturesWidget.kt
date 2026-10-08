package io.github.gauthiercpx.roundtrip.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import io.github.gauthiercpx.roundtrip.R

/** Placeholder content; the departures feed lands in phase 1 (docs/android-plan.md). */
class DeparturesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = context.getString(R.string.widget_empty))
            }
        }
    }
}

class DeparturesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeparturesWidget()
}
