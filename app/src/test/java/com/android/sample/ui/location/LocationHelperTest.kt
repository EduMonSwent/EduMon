package com.android.sample.ui.location

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for location resolution logic.
 * These tests ensure that the location selection logic is properly covered.
 */
class LocationHelperTest {

  @Test
  fun chooseLocation_true_returnsChosenCoordinates() {
    // Arrange
    val chosenLocation = LatLng(46.5191, 6.5668) // EPFL coordinates
    val actualLocation: Pair<Double, Double>? = 47.3769 to 8.5417 // Zurich

    // Act
    val result = resolveLocationCoordinates(true, chosenLocation, actualLocation)

    // Assert
    assertEquals(46.5191, result?.first ?: 0.0, 0.0001)
    assertEquals(6.5668, result?.second ?: 0.0, 0.0001)
  }

  @Test
  fun chooseLocation_false_withActualLocation_returnsActualLocation() {
    // Arrange
    val chosenLocation = LatLng(46.5191, 6.5668) // EPFL
    val actualLocation: Pair<Double, Double>? = 47.3769 to 8.5417 // Zurich

    // Act
    val result = resolveLocationCoordinates(false, chosenLocation, actualLocation)

    // Assert
    assertEquals(47.3769, result?.first ?: 0.0, 0.0001)
    assertEquals(8.5417, result?.second ?: 0.0, 0.0001)
  }

  @Test
  fun chooseLocation_false_withNullActualLocation_returnsNull() {
    // Arrange
    val chosenLocation = LatLng(46.5191, 6.5668)
    val actualLocation: Pair<Double, Double>? = null

    // Act
    val result = resolveLocationCoordinates(false, chosenLocation, actualLocation)

    // Assert
    assertNull(result)
  }

  @Test
  fun chooseLocation_true_withNullActualLocation_stillReturnsChosenLocation() {
    // Arrange
    val chosenLocation = LatLng(46.5191, 6.5668)
    val actualLocation: Pair<Double, Double>? = null

    // Act
    val result = resolveLocationCoordinates(true, chosenLocation, actualLocation)

    // Assert
    assertEquals(46.5191, result?.first ?: 0.0, 0.0001)
    assertEquals(6.5668, result?.second ?: 0.0, 0.0001)
  }

  @Test
  fun edgeCase_zeroCoordinates() {
    // Arrange
    val chosenLocation = LatLng(0.0, 0.0)
    val actualLocation: Pair<Double, Double>? = 0.0 to 0.0

    // Act
    val resultChosen = resolveLocationCoordinates(true, chosenLocation, actualLocation)
    val resultActual = resolveLocationCoordinates(false, chosenLocation, actualLocation)

    // Assert
    assertEquals(0.0, resultChosen?.first ?: -1.0, 0.0001)
    assertEquals(0.0, resultChosen?.second ?: -1.0, 0.0001)
    assertEquals(0.0, resultActual?.first ?: -1.0, 0.0001)
    assertEquals(0.0, resultActual?.second ?: -1.0, 0.0001)
  }

  @Test
  fun edgeCase_negativeCoordinates() {
    // Arrange
    val chosenLocation = LatLng(-33.8688, 151.2093) // Sydney (southern hemisphere)
    val actualLocation: Pair<Double, Double>? = -34.6037 to -58.3816 // Buenos Aires

    // Act
    val resultChosen = resolveLocationCoordinates(true, chosenLocation, actualLocation)
    val resultActual = resolveLocationCoordinates(false, chosenLocation, actualLocation)

    // Assert
    assertEquals(-33.8688, resultChosen?.first ?: 0.0, 0.0001)
    assertEquals(151.2093, resultChosen?.second ?: 0.0, 0.0001)
    assertEquals(-34.6037, resultActual?.first ?: 0.0, 0.0001)
    assertEquals(-58.3816, resultActual?.second ?: 0.0, 0.0001)
  }
}

