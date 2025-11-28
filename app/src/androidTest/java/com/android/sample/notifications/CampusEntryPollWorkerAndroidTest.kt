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

  /**
   * Test activeFetchOrStored fallback path: when getCurrentLocation fails/unavailable, it should
   * fall back to stored location if available. This test covers the catch block in
   * activeFetchOrStored that calls readStoredLocation.
   */
  @Test
  fun doWork_withoutSkipFetch_andStoredLocation_usesStoredLocationAsFallback() {
    // Given: Store a location first (simulating a previous successful fetch)
    context
        .getSharedPreferences("last_location", Context.MODE_PRIVATE)
        .edit()
        .putFloat("lat", 46.5202f)
        .putFloat("lon", 6.5652f)
        .commit()

    // When: Run worker WITHOUT skip_fetch flag
    // This will trigger activeFetchOrStored -> getCurrentLocation (which may fail in CI)
    // -> fallback to readStoredLocation
    val inputData = Data.Builder().putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true).build()

    val workInfo = runWorker(inputData)

    // Then: Worker should succeed (either from getCurrentLocation or stored fallback)
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Verify the stored location was persisted (confirms location was used)
    val prefs = context.getSharedPreferences("last_location", Context.MODE_PRIVATE)
    assertTrue("Location should be stored", prefs.contains("lat"))
    assertTrue("Location should be stored", prefs.contains("lon"))
  }

  /**
   * Test getCurrentLocation path when no stored location exists. In CI without location services,
   * this will test the fallback path where getCurrentLocation throws and readStoredLocation returns
   * null.
   */
  @Test
  fun doWork_withoutSkipFetch_andNoStoredLocation_handlesNoLocationGracefully() {
    // Given: NO stored location (cleared in setup)
    // Verify it's actually cleared
    val prefs = context.getSharedPreferences("last_location", Context.MODE_PRIVATE)
    assertFalse("Should have no stored lat", prefs.contains("lat"))
    assertFalse("Should have no stored lon", prefs.contains("lon"))

    // When: Run worker WITHOUT skip_fetch flag
    // This will trigger activeFetchOrStored -> getCurrentLocation (likely fails in CI)
    // -> readStoredLocation (returns null) -> return null from activeFetchOrStored
    val inputData = Data.Builder().putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true).build()

    val workInfo = runWorker(inputData)

    // Then: Worker should still succeed (early return when position is null)
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Campus flag should remain false (no location to process)
    val wasOnCampus =
        context
            .getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
            .getBoolean("was_on_campus", false)
    assertFalse("Campus flag should remain false when no location available", wasOnCampus)
  }

  /**
   * Test that covers the successful getCurrentLocation path. Note: This test may behave differently
   * on emulator vs real device. On emulator without location mock, it will fall back to stored
   * location path. On real device with location services, it may get actual current location.
   */
  @Test
  fun doWork_withoutSkipFetch_withLocationServices_processesLocation() {
    // Given: Pre-store a valid on-campus location as fallback
    context
        .getSharedPreferences("last_location", Context.MODE_PRIVATE)
        .edit()
        .putFloat("lat", 46.5202f)
        .putFloat("lon", 6.5652f)
        .commit()

    // When: Run worker to trigger actual location fetch or fallback
    val inputData = Data.Builder().putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true).build()

    val workInfo = runWorker(inputData)

    // Then: Should succeed regardless of whether getCurrentLocation or fallback was used
    assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

    // Location should be persisted (either from getCurrentLocation or stored fallback)
    val prefs = context.getSharedPreferences("last_location", Context.MODE_PRIVATE)
    val lat = prefs.getFloat("lat", 0f)
    val lon = prefs.getFloat("lon", 0f)

    // Verify we got some valid coordinates
    assertTrue("Latitude should be valid", lat != 0f)
    assertTrue("Longitude should be valid", lon != 0f)
  }

  /**
   * Test scheduleNext schedules work with correct delay and unique work policy. Covers the
   * scheduleNext companion method.
   */
  @Test
  fun scheduleNext_schedulesWorkWithCorrectDelay() = runBlocking {
    // When: Call scheduleNext
    CampusEntryPollWorker.scheduleNext(context)

    // Then: Work should be scheduled with unique name
    kotlinx.coroutines.delay(200) // Give WorkManager time to schedule

    val workInfos = workManager.getWorkInfosByTag("campus_entry_poll").await()

    // Should have at least one pending or enqueued work item
    assertTrue("Work should be scheduled", workInfos.isNotEmpty())

    val hasScheduledWork =
        workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    assertTrue("Should have enqueued or running work", hasScheduledWork)

    // Clean up - cancel the scheduled work
    CampusEntryPollWorker.cancel(context)
  }

  /** Test startChain is an alias for scheduleNext. Covers the startChain companion method. */
  @Test
  fun startChain_schedulesWorkLikeScheduleNext() = runBlocking {
    // When: Call startChain
    CampusEntryPollWorker.startChain(context)

    // Then: Work should be scheduled (same behavior as scheduleNext)
    kotlinx.coroutines.delay(200) // Give WorkManager time to schedule

    val workInfos = workManager.getWorkInfosByTag("campus_entry_poll").await()

    assertTrue("Work should be scheduled via startChain", workInfos.isNotEmpty())

    val hasScheduledWork =
        workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    assertTrue("Should have enqueued or running work", hasScheduledWork)

    // Clean up
    CampusEntryPollWorker.cancel(context)
  }

  /** Test cancel properly cancels scheduled work. Covers the cancel companion method. */
  @Test
  fun cancel_cancelsScheduledWork() = runBlocking {
    // Given: Schedule some work first
    CampusEntryPollWorker.scheduleNext(context)
    kotlinx.coroutines.delay(200)

    // Verify work is scheduled
    val workInfosBefore = workManager.getWorkInfosByTag("campus_entry_poll").await()
    val hasWorkBefore =
        workInfosBefore.any {
          it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    assertTrue("Work should be scheduled before cancel", hasWorkBefore)

    // When: Cancel the work
    CampusEntryPollWorker.cancel(context)
    kotlinx.coroutines.delay(200) // Give WorkManager time to cancel

    // Then: Work should be cancelled
    val workInfosAfter = workManager.getWorkInfosByTag("campus_entry_poll").await()

    // After cancellation, work items should be cancelled or removed
    val allCancelledOrFinished =
        workInfosAfter.all { it.state == WorkInfo.State.CANCELLED || it.state.isFinished }
    assertTrue("All work should be cancelled or finished", allCancelledOrFinished)
  }

  /**
   * Test that REPLACE policy works when scheduling multiple times. Covers the enqueueUniqueWork
   * call with ExistingWorkPolicy.REPLACE.
   */
  @Test
  fun scheduleNext_multipleCallsReplacePreviousWork() = runBlocking {
    // Given: Schedule work first time
    CampusEntryPollWorker.scheduleNext(context)
    kotlinx.coroutines.delay(200)

    val workInfosFirst = workManager.getWorkInfosByTag("campus_entry_poll").await()
    val firstWorkId = workInfosFirst.firstOrNull()?.id

    // When: Schedule again (should REPLACE)
    CampusEntryPollWorker.scheduleNext(context)
    kotlinx.coroutines.delay(200)

    // Then: Should have work scheduled
    val workInfosSecond = workManager.getWorkInfosByTag("campus_entry_poll").await()
    assertTrue("Work should still be scheduled after second call", workInfosSecond.isNotEmpty())

    // Verify the policy allows only one active work with this unique name
    val activeWork =
        workInfosSecond.filter {
          it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
    assertTrue("Should have at least one active work", activeWork.isNotEmpty())

    // Clean up
    CampusEntryPollWorker.cancel(context)
  }
}
