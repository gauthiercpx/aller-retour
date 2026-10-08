package io.github.gauthiercpx.roundtrip.domain

import java.time.LocalTime

/** Phase 1 stand-in for the context engine: before noon is the outbound trip, after is the return. */
enum class TimeSlot { MORNING, EVENING }

private const val NOON_HOUR = 12

fun timeSlotOf(time: LocalTime): TimeSlot = if (time.hour < NOON_HOUR) TimeSlot.MORNING else TimeSlot.EVENING
