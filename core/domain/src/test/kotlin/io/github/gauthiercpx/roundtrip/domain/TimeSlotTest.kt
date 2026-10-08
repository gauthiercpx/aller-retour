package io.github.gauthiercpx.roundtrip.domain

import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeSlotTest {
    @Test
    fun midnightIsMorning() {
        assertEquals(TimeSlot.MORNING, timeSlotOf(LocalTime.MIDNIGHT))
    }

    @Test
    fun oneMinuteBeforeNoonIsMorning() {
        assertEquals(TimeSlot.MORNING, timeSlotOf(LocalTime.of(11, 59)))
    }

    @Test
    fun noonIsEvening() {
        assertEquals(TimeSlot.EVENING, timeSlotOf(LocalTime.NOON))
    }

    @Test
    fun stopPlanReturnsTheStopsOfTheRequestedSlot() {
        val plan = StopPlan(morning = listOf("idfm:1"), evening = listOf("star-metro:2"))

        assertEquals(listOf("idfm:1"), plan.stopsFor(TimeSlot.MORNING))
        assertEquals(listOf("star-metro:2"), plan.stopsFor(TimeSlot.EVENING))
    }
}
