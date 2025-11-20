package com.android.sample.ui.location

import com.google.android.gms.maps.model.LatLng

/**
 * Pure function to resolve which location coordinates should be used.
 *
 * @param chooseLocation If true, use the chosen location; otherwise use actual device location
 * @param chosenLocation The manually chosen location coordinates
 * @param actualLocation The actual device location from GPS (nullable)
 * @return The coordinates to use, or null if no location is available
 */
internal fun resolveLocationCoordinates(
    chooseLocation: Boolean,
    chosenLocation: LatLng,
    actualLocation: Pair<Double, Double>?
): Pair<Double, Double>? {
  return if (chooseLocation) {
    chosenLocation.latitude to chosenLocation.longitude
  } else {
    actualLocation
  }
}

