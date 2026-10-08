package io.github.gauthiercpx.roundtrip.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.gauthiercpx.roundtrip.R
import io.github.gauthiercpx.roundtrip.domain.UpcomingDeparture
import io.github.gauthiercpx.roundtrip.domain.selectUpcoming
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import io.github.gauthiercpx.roundtrip.model.DeparturesResponse
import io.github.gauthiercpx.roundtrip.model.Network
import io.github.gauthiercpx.roundtrip.ui.theme.IdfmBlue
import io.github.gauthiercpx.roundtrip.ui.theme.Ink
import io.github.gauthiercpx.roundtrip.ui.theme.Paper
import io.github.gauthiercpx.roundtrip.ui.theme.Slate
import io.github.gauthiercpx.roundtrip.ui.theme.StarRed
import java.time.Instant
import java.time.ZoneId

private const val ROWS_SHOWN = 2
private val ROW_GAP = 12.dp

private val TextPrimary = ColorProvider(day = Ink, night = Paper)
private val TextMuted = ColorProvider(day = Slate, night = Color(0xFFA9B1BC))
private val Background = ColorProvider(day = Paper, night = Ink)

@Composable
fun DeparturesWidgetContent(response: DeparturesResponse?, now: Instant, modifier: GlanceModifier = GlanceModifier) {
    val zone = ZoneId.systemDefault()
    val upcoming = response?.let { selectUpcoming(it, now, ROWS_SHOWN) }.orEmpty()

    Column(
        modifier = modifier.background(Background).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (upcoming.isEmpty()) {
            Text(
                text = LocalContext.current.getString(R.string.widget_empty),
                style = TextStyle(color = TextPrimary, fontSize = 15.sp),
            )
        } else {
            upcoming.forEach { DepartureRow(it, zone) }
        }
        response?.let { UpdatedFooter(it.generatedAt, zone) }
    }
}

@Composable
private fun DepartureRow(item: UpcomingDeparture, zone: ZoneId) {
    val departure = item.departure
    val context = LocalContext.current
    val badgeColor = parseLineColor(departure.lineColor)?.let { Color(it) } ?: fallbackLineColor(departure.network)
    val badgeText = if (prefersDarkText(badgeColor.toArgb())) Ink else Color.White
    val note = when {
        departure.status == DepartureStatus.CANCELLED -> context.getString(R.string.widget_cancelled)
        else -> delayMinutes(departure)?.let { context.getString(R.string.widget_delay_minutes, it) }
    }

    // Top-aligned so the destination and the time share the first line, whether or not a note sits below.
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = GlanceModifier.padding(top = 1.dp),
        ) {
            Box(
                modifier = GlanceModifier.background(badgeColor).cornerRadius(6.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = departure.lineName,
                    style = TextStyle(
                        color = ColorProvider(badgeText),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.width(ROW_GAP))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = departure.destination,
                maxLines = 1,
                style = TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            )
            note?.let { Text(text = it, maxLines = 1, style = TextStyle(color = TextMuted, fontSize = 11.sp)) }
        }
        Spacer(GlanceModifier.width(ROW_GAP))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = clockWithRealtimeMarker(departure, zone),
                style = TextStyle(
                    color = if (item.isStale) TextMuted else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            // Decorative: the line and destination already say everything the picture does.
            Image(
                provider = ImageProvider(vehicleArt(departure)),
                contentDescription = null,
                colorFilter = ColorFilter.tint(TextMuted),
                modifier = GlanceModifier.padding(top = 3.dp).size(width = 30.dp, height = 12.dp),
            )
        }
    }
}

@Composable
private fun UpdatedFooter(generatedAt: Instant, zone: ZoneId) {
    Text(
        text = LocalContext.current.getString(R.string.widget_updated, formatClock(generatedAt, zone)),
        style = TextStyle(color = TextMuted, fontSize = 10.sp),
        modifier = GlanceModifier.padding(top = 4.dp),
    )
}

private fun fallbackLineColor(network: Network): Color = when (network) {
    Network.STAR -> StarRed
    Network.IDFM, Network.SNCF -> IdfmBlue
}
