package io.github.gauthiercpx.roundtrip.widget

import androidx.annotation.DrawableRes
import io.github.gauthiercpx.roundtrip.R
import io.github.gauthiercpx.roundtrip.model.Departure
import io.github.gauthiercpx.roundtrip.model.Mode

/**
 * The vehicle picture shown next to a departure. For now one generic silhouette per family; the exact
 * illustrations will be picked from `departure.vehicle.modelId` here, falling back to these placeholders when
 * the model is unknown.
 */
@DrawableRes
fun vehicleArt(departure: Departure): Int = when (departure.mode) {
    Mode.BUS -> R.drawable.vehicle_placeholder_bus
    Mode.METRO, Mode.RER, Mode.TRAIN, Mode.TRAM -> R.drawable.vehicle_placeholder_train
}
