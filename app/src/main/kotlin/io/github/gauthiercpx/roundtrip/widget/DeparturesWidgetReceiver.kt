package io.github.gauthiercpx.roundtrip.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class DeparturesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeparturesWidget()

    // Refreshing only while a widget exists keeps the app idle for users who removed it.
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RefreshScheduler.schedulePeriodic(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RefreshScheduler.cancelPeriodic(context)
    }
}
