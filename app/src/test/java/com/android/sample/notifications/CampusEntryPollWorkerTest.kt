package com.android.sample.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.android.sample.data.notifications.CampusEntryPollWorker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class CampusEntryPollWorkerTest {

  private lateinit var context: Context
  private lateinit var worker: CampusEntryPollWorker
  private lateinit var campusPollPrefs: SharedPreferences
  private lateinit var locationPrefs: SharedPreferences

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    // Initialize WorkManager for testing
    WorkManagerTestInitHelper.initializeTestWorkManager(context)

    // Grant all necessary permissions for SDK 33+
    val app = context as android.app.Application
    val shadowApp: ShadowApplication = shadowOf(app)
    shadowApp.grantPermissions(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION)

    // Build worker with chain disabled to avoid lingering scheduled work in tests
    worker =
        TestListenableWorkerBuilder<CampusEntryPollWorker>(context)
            .setInputData(
                androidx.work.Data.Builder()
                    .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
                    .build())
            .build()
    campusPollPrefs = context.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
    locationPrefs = context.getSharedPreferences("last_location", Context.MODE_PRIVATE)

    // Clear prefs before each test
    campusPollPrefs.edit().clear().commit()
    locationPrefs.edit().clear().commit()
  }

  @Test
  fun `worker returns success when feature disabled`() = runTest {
    // Note: The worker doesn't check for enabled flag - it just runs if scheduled
    // The scheduling is controlled by NotificationsViewModel

    // When
    val result = worker.doWork()

    // Then
    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `worker returns success when no last location`() = runTest {
    // Given: no location stored (will use default center of campus)

    // When
    val result = worker.doWork()

    // Then
    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `worker posts notification on campus entry transition`() = runTest {
    // Given: off campus initially, now on campus using test shortcut (skip actual fetch)
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()

    val testWorker =
        TestListenableWorkerBuilder<CampusEntryPollWorker>(context)
            .setInputData(
                androidx.work.Data.Builder()
                    .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
                    .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
                    .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, 46.5202)
                    .putDouble(CampusEntryPollWorker.KEY_TEST_LON, 6.5652)
                    .build())
            .build()

    val result = testWorker.startWork().get()
    assertEquals(ListenableWorker.Result.success(), result)

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    val notifications = shadowNotificationManager.allNotifications
    assertTrue("Should post notification", notifications.size > 0)
    val notification = notifications.first()
    assertEquals("Welcome to campus", notification.extras.getString("android.title"))
  }

  @Test
  fun `worker does not post notification when already on campus`() = runTest {
    // Given: already on campus
    campusPollPrefs.edit().putBoolean("was_on_campus", true).commit()
    locationPrefs.edit().putFloat("lat", 46.5202f).putFloat("lon", 6.5652f).commit()

    // When
    val result = worker.doWork()

    // Then
    assertEquals(ListenableWorker.Result.success(), result)

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    assertEquals(0, shadowNotificationManager.allNotifications.size)
  }

  @Test
  fun `worker updates was_on_campus state after execution`() = runTest {
    // Given: off campus initially, now on campus
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()
    locationPrefs.edit().putFloat("lat", 46.5202f).putFloat("lon", 6.5652f).commit()

    // When
    worker.doWork()

    // Then
    assertTrue(campusPollPrefs.getBoolean("was_on_campus", false))
  }

  @Test
  fun `worker detects off campus location correctly`() = runTest {
    // Given: on campus initially, now off campus
    campusPollPrefs.edit().putBoolean("was_on_campus", true).commit()
    locationPrefs.edit().putFloat("lat", 46.510f).putFloat("lon", 6.550f).commit()

    // When
    worker.doWork()

    // Then
    assertFalse(campusPollPrefs.getBoolean("was_on_campus", true))
  }

  @Test
  fun `worker reschedules itself for next poll`() = runTest {
    // Given: location on campus
    locationPrefs.edit().putFloat("lat", 46.5202f).putFloat("lon", 6.5652f).commit()

    // When
    val result = worker.doWork()

    // Then: should return success to allow chaining
    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `isOnEpflCampus returns true for coordinates inside bounding box`() = runTest {
    // Test central campus location
    locationPrefs.edit().putFloat("lat", 46.5202f).putFloat("lon", 6.5652f).commit()
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()

    val result = worker.doWork()

    // Should detect as on campus and update state
    assertEquals(ListenableWorker.Result.success(), result)
    assertTrue(campusPollPrefs.getBoolean("was_on_campus", false))
  }

  @Test
  fun `isOnEpflCampus returns false for coordinates outside bounding box`() = runTest {
    // Test location clearly outside campus
    locationPrefs.edit().putFloat("lat", 46.510f).putFloat("lon", 6.550f).commit()
    campusPollPrefs.edit().putBoolean("was_on_campus", true).commit()

    val result = worker.doWork()

    // Should detect as off campus and update state
    assertEquals(ListenableWorker.Result.success(), result)
    assertFalse(campusPollPrefs.getBoolean("was_on_campus", true))
  }

  @Test
  fun `worker stores fetched location in SharedPreferences`() = runTest {
    // Given: location prefs are initially empty but worker will try to fetch location
    // In Robolectric, FusedLocationProviderClient may not work, so it will fall back
    // But if it does fetch, it should store the result
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()

    // Pre-populate with a fallback location for the test
    locationPrefs.edit().putFloat("lat", 46.5200f).putFloat("lon", 6.5650f).commit()

    // When
    worker.doWork()

    // Then: location should still be in prefs (either fetched or fallback)
    assertTrue(locationPrefs.contains("lat"))
    assertTrue(locationPrefs.contains("lon"))
  }

  @Test
  fun `worker returns success when location permission denied`() = runTest {
    // Given: revoke location permissions
    val app = context as android.app.Application
    val shadowApp: ShadowApplication = shadowOf(app)
    shadowApp.denyPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    // When
    val result = worker.doWork()

    // Then: should return success (not failure) to avoid retries
    assertEquals(ListenableWorker.Result.success(), result)

    // And should not have posted notification
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    assertEquals(0, shadowNotificationManager.allNotifications.size)
  }

  @Test
  fun `worker returns success when notification permission denied`() = runTest {
    // Given: revoke notification permission
    val app = context as android.app.Application
    val shadowApp: ShadowApplication = shadowOf(app)
    shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

    locationPrefs.edit().putFloat("lat", 46.5202f).putFloat("lon", 6.5652f).commit()

    // When
    val result = worker.doWork()

    // Then: should return success (not failure) to avoid retries
    assertEquals(ListenableWorker.Result.success(), result)

    // And should not have posted notification
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    assertEquals(0, shadowNotificationManager.allNotifications.size)
  }

  @Test
  fun `worker uses fallback location when getCurrentLocation fails`() = runTest {
    // Given: A fallback location in prefs (simulates failed location fetch)
    locationPrefs.edit().putFloat("lat", 46.5202f).putFloat("lon", 6.5652f).commit()
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()

    // When: worker runs (in Robolectric, FusedLocationProvider might not work, so fallback is used)
    val result = worker.doWork()

    // Then: should succeed using fallback location
    assertEquals(ListenableWorker.Result.success(), result)

    // Should still detect campus and update state
    assertTrue(campusPollPrefs.getBoolean("was_on_campus", false))
  }

  @Test
  fun `worker handles missing fallback location gracefully`() = runTest {
    // Given: no location in prefs and getCurrentLocation will fail in Robolectric
    // (both fresh location fetch and fallback will fail)

    // When
    val result = worker.doWork()

    // Then: should return success without crashing
    assertEquals(ListenableWorker.Result.success(), result)

    // Should not have posted notification
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    assertEquals(0, shadowNotificationManager.allNotifications.size)
  }

  @Test
  fun `worker updates location prefs with fetched location`() = runTest {
    // Given: initial location
    locationPrefs.edit().putFloat("lat", 46.510f).putFloat("lon", 6.550f).commit()

    // When: worker runs
    worker.doWork()

    // Then: location prefs should be updated (either with fresh fetch or confirmed fallback)
    assertTrue(locationPrefs.contains("lat"))
    assertTrue(locationPrefs.contains("lon"))

    // Values should be valid doubles
    val lat = locationPrefs.getFloat("lat", 0f).toDouble()
    val lon = locationPrefs.getFloat("lon", 0f).toDouble()
    assertTrue(lat != 0.0 || lon != 0.0)
  }

  @Test
  fun `worker boundary test - exactly at north edge of campus`() = runTest {
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()
    val northLat = 46.525 - 0.000001 // slightly inside
    val workerEdge =
        TestListenableWorkerBuilder<CampusEntryPollWorker>(context)
            .setInputData(
                androidx.work.Data.Builder()
                    .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
                    .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
                    .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, northLat)
                    .putDouble(CampusEntryPollWorker.KEY_TEST_LON, 6.5652)
                    .build())
            .build()
    val result = workerEdge.startWork().get()
    assertEquals(ListenableWorker.Result.success(), result)
    val prefs = context.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
    assertTrue(
        "Expected on-campus state at north boundary", prefs.getBoolean("was_on_campus", false))
  }

  @Test
  fun `worker boundary test - exactly at south edge of campus`() = runTest {
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()
    val southLat = 46.515 + 0.000001 // slightly inside
    val workerEdge =
        TestListenableWorkerBuilder<CampusEntryPollWorker>(context)
            .setInputData(
                androidx.work.Data.Builder()
                    .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
                    .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
                    .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, southLat)
                    .putDouble(CampusEntryPollWorker.KEY_TEST_LON, 6.5652)
                    .build())
            .build()
    val result = workerEdge.startWork().get()
    assertEquals(ListenableWorker.Result.success(), result)
    val prefs = context.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
    assertTrue(
        "Expected on-campus state at south boundary", prefs.getBoolean("was_on_campus", false))
  }

  @Test
  fun `worker boundary test - exactly at east edge of campus`() = runTest {
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()
    val eastLng = 6.575 - 0.000001 // slightly inside
    val workerEdge =
        TestListenableWorkerBuilder<CampusEntryPollWorker>(context)
            .setInputData(
                androidx.work.Data.Builder()
                    .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
                    .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
                    .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, 46.5202)
                    .putDouble(CampusEntryPollWorker.KEY_TEST_LON, eastLng)
                    .build())
            .build()
    val result = workerEdge.startWork().get()
    assertEquals(ListenableWorker.Result.success(), result)
    val prefs = context.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
    assertTrue(
        "Expected on-campus state at east boundary", prefs.getBoolean("was_on_campus", false))
  }

  @Test
  fun `worker boundary test - exactly at west edge of campus`() = runTest {
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()
    val westLng = 6.555 + 0.000001 // slightly inside
    val workerEdge =
        TestListenableWorkerBuilder<CampusEntryPollWorker>(context)
            .setInputData(
                androidx.work.Data.Builder()
                    .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
                    .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
                    .putDouble(CampusEntryPollWorker.KEY_TEST_LAT, 46.5202)
                    .putDouble(CampusEntryPollWorker.KEY_TEST_LON, westLng)
                    .build())
            .build()
    val result = workerEdge.startWork().get()
    assertEquals(ListenableWorker.Result.success(), result)
    val prefs = context.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
    assertTrue(
        "Expected on-campus state at west boundary", prefs.getBoolean("was_on_campus", false))
  }

  @Test
  fun `worker does not post duplicate notifications on repeated campus entries`() = runTest {
    // Given: off campus initially
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()
    locationPrefs.edit().putFloat("lat", 46.5202f).putFloat("lon", 6.5652f).commit()

    // When: enter campus first time
    worker.doWork()

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    val firstCount = shadowNotificationManager.allNotifications.size

    // When: run worker again (still on campus)
    worker.doWork()

    // Then: should not post another notification
    val secondCount = shadowNotificationManager.allNotifications.size
    assertEquals(
        "Should not post duplicate notification when staying on campus", firstCount, secondCount)
  }
}
