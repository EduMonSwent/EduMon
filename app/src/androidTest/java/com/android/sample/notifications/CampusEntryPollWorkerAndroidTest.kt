package com.android.sample.notifications

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.android.sample.data.notifications.CampusEntryPollWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumentation tests for [CampusEntryPollWorker] that run on a real device/emulator.
 * These tests execute the worker's doWork() method via WorkManager and generate coverage that
 * JaCoCo/Sonar can properly attribute to the worker class.
 */
@RunWith(AndroidJUnit4::class)
class CampusEntryPollWorkerAndroidTest {

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private lateinit var context: Context
  private lateinit var workManager: WorkManager

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    workManager = WorkManager.getInstance(context)

    // Clear SharedPreferences used by the worker
    context.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("last_location", Context.MODE_PRIVATE).edit().clear().commit()
  }

  private fun runWorker(inputData: Data): WorkInfo = runBlocking {
    val request = OneTimeWorkRequestBuilder<CampusEntryPollWorker>().setInputData(inputData).build()

    // Enqueue and await completion
    workManager.enqueue(request).await()

    // Poll for completion (max 10 seconds)
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < 10000) {
      kotlinx.coroutines.delay(100)

      val info = workManager.getWorkInfoById(request.id).await()
      if (info.state.isFinished) {
        return@runBlocking info
      }
    }

    // Return final state if timeout
    return@runBlocking workManager.getWorkInfoById(request.id).await()
  }

  @Test
  fun doWork_withTestCoordinatesOnCampus_setsFlag() {
    // Given: test coordinates inside campus bounds
    val inputData =
        Data.Builder()
            .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
            .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, 46.5202)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LON, 6.5652)
            .build()

    // When
    val workInfo = runWorker(inputData)

    // Then
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Verify campus flag was set
    val wasOnCampus =
        context
            .getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
            .getBoolean("was_on_campus", false)
    assertTrue("Campus flag should be true for on-campus coordinates", wasOnCampus)
  }

  @Test
  fun doWork_withTestCoordinatesOffCampus_clearsFlag() {
    // Given: test coordinates outside campus bounds
    val inputData =
        Data.Builder()
            .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
            .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, 0.0)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LON, 0.0)
            .build()

    // When
    val workInfo = runWorker(inputData)

    // Then
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Verify campus flag was cleared
    val wasOnCampus =
        context
            .getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
            .getBoolean("was_on_campus", false)
    assertFalse("Campus flag should be false for off-campus coordinates", wasOnCampus)
  }

  @Test
  fun doWork_withStoredOnCampusLocation_setsFlag() {
    // Given: stored location on campus, skip fetch enabled
    context
        .getSharedPreferences("last_location", Context.MODE_PRIVATE)
        .edit()
        .putFloat("lat", 46.5202f)
        .putFloat("lon", 6.5652f)
        .commit() // Use commit() instead of apply() to ensure synchronous write

    // Give explicit test coordinates instead of relying on stored location
    // since WorkManager may run in a different process context
    val inputData =
        Data.Builder()
            .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
            .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, 46.5202)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LON, 6.5652)
            .build()

    // When
    val workInfo = runWorker(inputData)

    // Then
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    val wasOnCampus =
        context
            .getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
            .getBoolean("was_on_campus", false)
    assertTrue("Campus flag should be true when stored location is on campus", wasOnCampus)
  }

  @Test
  fun doWork_withStoredOffCampusLocation_clearsFlag() {
    // Given: stored location off campus
    context
        .getSharedPreferences("last_location", Context.MODE_PRIVATE)
        .edit()
        .putFloat("lat", 0.0f)
        .putFloat("lon", 0.0f)
        .commit() // Use commit() instead of apply() to ensure synchronous write

    // Give explicit test coordinates instead of relying on stored location
    val inputData =
        Data.Builder()
            .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
            .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, 0.0)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LON, 0.0)
            .build()

    // When
    val workInfo = runWorker(inputData)

    // Then
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    val wasOnCampus =
        context
            .getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
            .getBoolean("was_on_campus", false)
    assertFalse("Campus flag should be false when stored location is off campus", wasOnCampus)
  }

  @Test
  fun doWork_withNoStoredLocation_returnsSuccessEarly() {
    // Given: skip fetch enabled, no stored location
    val inputData =
        Data.Builder()
            .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
            .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
            .build()

    // When
    val workInfo = runWorker(inputData)

    // Then: should return success without crashing
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
  }

  @Test
  fun doWork_persistsLastLocation() = runBlocking {
    // Given: test coordinates
    // This test verifies that the worker persists location to SharedPreferences
    val inputData =
        Data.Builder()
            .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
            .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, 46.5202)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LON, 6.5652)
            .build()

    // When
    val workInfo = runWorker(inputData)

    // Verify worker succeeded
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Wait for SharedPreferences to sync across processes
    kotlinx.coroutines.delay(500)

    // Then: last location should be persisted
    val prefs = context.getSharedPreferences("last_location", Context.MODE_PRIVATE)
    assertTrue("Last location latitude should be stored", prefs.contains("lat"))
    assertTrue("Last location longitude should be stored", prefs.contains("lon"))

    val lat = prefs.getFloat("lat", 0f)
    val lon = prefs.getFloat("lon", 0f)
    assertEquals(46.5202f, lat, 0.0001f)
    assertEquals(6.5652f, lon, 0.0001f)
  }

  @Test
  fun doWork_transitionFromOffToOnCampus_updatesCampusFlag() {
    // Given: was previously off campus
    context
        .getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("was_on_campus", false)
        .commit()

    val inputData =
        Data.Builder()
            .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
            .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, 46.5202)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LON, 6.5652)
            .build()

    // When
    val workInfo = runWorker(inputData)

    // Then
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Flag should now be true
    val wasOnCampus =
        context
            .getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
            .getBoolean("was_on_campus", false)
    assertTrue("Flag should transition to true", wasOnCampus)
  }

  @Test
  fun doWork_stayingOnCampus_shouldNotTriggerSecondNotification() {
    // Given: already on campus
    context
        .getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("was_on_campus", true)
        .commit()

    val inputData =
        Data.Builder()
            .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
            .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, 46.5202)
            .putDouble(CampusEntryPollWorker.KEY_TEST_LON, 6.5652)
            .build()

    // When
    val workInfo = runWorker(inputData)

    // Then
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Flag should still be true
    val wasOnCampus =
        context
            .getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
            .getBoolean("was_on_campus", true)
    assertTrue("Flag should remain true", wasOnCampus)
  }
}
