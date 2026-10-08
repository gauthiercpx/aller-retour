package io.github.gauthiercpx.roundtrip.domain

/** Backend stop ids to query for each [TimeSlot]; the stops carry their network (`idfm:`, `star-metro:`...). */
data class StopPlan(val morning: List<String> = emptyList(), val evening: List<String> = emptyList()) {
    fun stopsFor(slot: TimeSlot): List<String> = when (slot) {
        TimeSlot.MORNING -> morning
        TimeSlot.EVENING -> evening
    }
}
