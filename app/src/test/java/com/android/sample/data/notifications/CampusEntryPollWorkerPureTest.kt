package com.android.sample.data.notifications

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for helper logic inside CampusEntryPollWorker. These do not rely on Robolectric
 * and ensure JaCoCo always records some coverage on this class.
 */
class CampusEntryPollWorkerPureTest {

  @Test
  fun isOnEpflCampus_returns_true_for_inside_point() {
    val pos = LatLng(46.52, 6.565) // clearly inside the EPFL bounds used
    val inside = CampusEntryPollWorker.isOnEpflCampusInternal(pos)
    assertTrue(inside)
  }

  @Test
  fun isOnEpflCampus_returns_false_for_outside_point() {
    val pos = LatLng(0.0, 0.0) // clearly outside
    val outside = CampusEntryPollWorker.isOnEpflCampusInternal(pos)
    assertFalse(outside)
  }
}
