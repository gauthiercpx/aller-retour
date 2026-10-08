package io.github.gauthiercpx.roundtrip.widget

import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.DepartureStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val HEX_COLOR = Regex("[0-9a-fA-F]{6}")
private const val RADIX_HEX = 16
private const val OPAQUE = 0xFF000000L
private const val DARK_TEXT_LUMINANCE_THRESHOLD = 0.6
private const val SECONDS_PER_MINUTE = 60

/** Absolute clock time ("14:32"): the widget cannot tick, so countdowns would be wrong between refreshes. */
fun formatClock(instant: Instant, zone: ZoneId): String = CLOCK_FORMAT.format(instant.atZone(zone))

/** Prefix for a time that is only the theoretical schedule rather than a live estimate. */
fun clockWithRealtimeMarker(departure: Departure, zone: ZoneId): String {
    val clock = formatClock(departure.expectedTime, zone)
    return if (departure.isRealtime) clock else "~$clock"
}

/** Whole minutes of delay worth showing, or null when on time or unknown. */
fun delayMinutes(departure: Departure): Int? {
    val minutes = (departure.delaySeconds ?: return null) / SECONDS_PER_MINUTE
    return minutes.takeIf { it > 0 && departure.status != DepartureStatus.CANCELLED }
}

/** Parses `#RRGGBB` or `RRGGBB` into an opaque ARGB int; null for anything else. */
fun parseLineColor(raw: String?): Int? {
    val hex = raw?.trim()?.removePrefix("#")?.takeIf { HEX_COLOR.matches(it) }
    return hex?.let { (OPAQUE or it.toLong(RADIX_HEX)).toInt() }
}

/** Dark text on light line colors, white text on dark ones. */
fun prefersDarkText(argb: Int): Boolean {
    val red = (argb shr 16 and 0xFF) / 255.0
    val green = (argb shr 8 and 0xFF) / 255.0
    val blue = (argb and 0xFF) / 255.0
    val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
    return luminance > DARK_TEXT_LUMINANCE_THRESHOLD
}
