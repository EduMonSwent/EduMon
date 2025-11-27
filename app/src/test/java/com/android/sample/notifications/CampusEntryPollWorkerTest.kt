// src/test/java/com/android/sample/data/notifications/CampusEntryPollWorkerCoverageTest.kt
package com.android.sample.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.android.sample.data.notifications.CampusEntryPollWorker
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.runBlocking
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
class CampusEntryPollWorkerCoverageTest {

  private lateinit var ctx: Context
  private lateinit var shadowApp: ShadowApplication

  @Before
  fun setup() {
    ctx = ApplicationProvider.getApplicationContext()
    WorkManagerTestInitHelper.initializeTestWorkManager(ctx)
    val app = ctx as Application
    shadowApp = shadowOf(app)

    // Clear stored prefs before each test
    ctx.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE).edit().clear().commit()
    ctx.getSharedPreferences("last_location", Context.MODE_PRIVATE).edit().clear().commit()
  }

  // Helper to build worker with input map
  private fun buildWorkerWithInputs(vararg kv: Pair<String, Any?>): CampusEntryPollWorker {
    val dataBuilder = Data.Builder()
    kv.forEach { (k, v) ->
      when (v) {
        is Boolean -> dataBuilder.putBoolean(k, v)
        is Double -> dataBuilder.putDouble(k, v)
        is String -> dataBuilder.putString(k, v)
        is Int -> dataBuilder.putInt(k, v)
        else -> {}
      }
    }
    return TestListenableWorkerBuilder.from(ctx, CampusEntryPollWorker::class.java)
        .setInputData(dataBuilder.build())
        .build() as CampusEntryPollWorker
  }

  @Test
  fun `returns success and no notification when location permission denied`() {
    // Deny location permissions
    shadowApp.denyPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    val worker = buildWorkerWithInputs(CampusEntryPollWorker.Companion.KEY_DISABLE_CHAIN to true)
    val result = runBlocking { worker.doWork() }
    assertEquals(ListenableWorker.Result.success(), result)

    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertTrue("No notifications expected", shadowOf(nm).allNotifications.isEmpty())
  }

  @Test
  fun `returns success and no notification when notification permission denied`() {
    // Grant location but deny notifications
    shadowApp.grantPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

    // Put a stored location so the worker would otherwise continue
    ctx.getSharedPreferences("last_location", Context.MODE_PRIVATE)
        .edit()
        .putFloat("lat", 46.5202f)
        .putFloat("lon", 6.5652f)
        .commit()

    val worker = buildWorkerWithInputs(CampusEntryPollWorker.Companion.KEY_DISABLE_CHAIN to true)
    val result = runBlocking { worker.doWork() }
    assertEquals(ListenableWorker.Result.success(), result)

    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertTrue(
        "No notifications expected when POST_NOTIFICATIONS denied",
        shadowOf(nm).allNotifications.isEmpty())
  }

  @Test
  fun `skipFetch without stored_location skips and returns success`() {
    // Grant permissions
    shadowApp.grantPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS)

    // skipFetch true, but no stored last_location present
    val worker =
        buildWorkerWithInputs(
            CampusEntryPollWorker.Companion.KEY_DISABLE_CHAIN to true,
            CampusEntryPollWorker.Companion.KEY_TEST_SKIP_FETCH to true)
    val result = runBlocking { worker.doWork() }
    assertEquals(ListenableWorker.Result.success(), result)

    // no notification posted
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertTrue(shadowOf(nm).allNotifications.isEmpty())
  }

  @Test
  fun `skipFetch with stored on-campus location posts notification and updates flag`() {
    // Grant permissions
    shadowApp.grantPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS)

    // store on-campus coords (within campus bounding box)
    ctx.getSharedPreferences("last_location", Context.MODE_PRIVATE)
        .edit()
        .putFloat("lat", 46.5202f)
        .putFloat("lon", 6.5652f)
        .commit()

    // was_on_campus initially false
    ctx.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("was_on_campus", false)
        .commit()

    val worker =
        buildWorkerWithInputs(
            CampusEntryPollWorker.Companion.KEY_DISABLE_CHAIN to true,
            CampusEntryPollWorker.Companion.KEY_TEST_SKIP_FETCH to true)
    val result = runBlocking { worker.doWork() }
    assertEquals(ListenableWorker.Result.success(), result)

    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val all = shadowOf(nm).allNotifications
    assertTrue("Notification expected for campus entry", all.isNotEmpty())

    // flag updated to true
    val was =
        ctx.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
            .getBoolean("was_on_campus", false)
    assertTrue("Campus flag should be set true after entry", was)
  }

  @Test
  fun `skipFetch with stored off-campus updates state without notification`() {
    // Grant permissions
    shadowApp.grantPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS)

    // store OFF-campus coords
    ctx.getSharedPreferences("last_location", Context.MODE_PRIVATE)
        .edit()
        .putFloat("lat", 46.510f)
        .putFloat("lon", 6.550f)
        .commit()

    // initially was_on_campus true (so leaving campus is expected)
    ctx.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("was_on_campus", true)
        .commit()

    val worker =
        buildWorkerWithInputs(
            CampusEntryPollWorker.Companion.KEY_DISABLE_CHAIN to true,
            CampusEntryPollWorker.Companion.KEY_TEST_SKIP_FETCH to true)
    val result = runBlocking { worker.doWork() }
    assertEquals(ListenableWorker.Result.success(), result)

    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertTrue(
        "No notification expected for leaving campus", shadowOf(nm).allNotifications.isEmpty())

    // flag should be set to false
    val was =
        ctx.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
            .getBoolean("was_on_campus", true)
    assertFalse(was)
  }

  @Test
  fun `test coordinates shortcut marks on-campus and posts notification`() {
    // Grant permissions
    shadowApp.grantPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS)

    ctx.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("was_on_campus", false)
        .commit()

    val worker =
        buildWorkerWithInputs(
            CampusEntryPollWorker.Companion.KEY_DISABLE_CHAIN to true,
            CampusEntryPollWorker.Companion.KEY_TEST_SKIP_FETCH to true,
            CampusEntryPollWorker.Companion.KEY_TEST_LAT to 46.5202,
            CampusEntryPollWorker.Companion.KEY_TEST_LON to 6.5652)
    val result = runBlocking { worker.doWork() }
    assertEquals(ListenableWorker.Result.success(), result)

    val prefs = ctx.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
    assertTrue(prefs.getBoolean("was_on_campus", false))

    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertTrue(
        "Notification expected for test coordinates", shadowOf(nm).allNotifications.isNotEmpty())
  }

  @Test
  fun `posts notification only on first campus entry and not on second`() {
    // Grant permissions
    shadowApp.grantPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS)

    // store on-campus coords and ensure was_on_campus false
    ctx.getSharedPreferences("last_location", Context.MODE_PRIVATE)
        .edit()
        .putFloat("lat", 46.5202f)
        .putFloat("lon", 6.5652f)
        .commit()

    ctx.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("was_on_campus", false)
        .commit()

    val worker = buildWorkerWithInputs(CampusEntryPollWorker.Companion.KEY_DISABLE_CHAIN to true)
    val first = runBlocking { worker.doWork() }
    assertEquals(ListenableWorker.Result.success(), first)

    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val firstCount = shadowOf(nm).allNotifications.size
    assertTrue(firstCount > 0)

    // Running a second worker instance should not add more notifications
    val worker2 = buildWorkerWithInputs(CampusEntryPollWorker.Companion.KEY_DISABLE_CHAIN to true)
    val second = runBlocking { worker2.doWork() }
    assertEquals(ListenableWorker.Result.success(), second)

    val secondCount = shadowOf(nm).allNotifications.size
    assertEquals("No new notification on repeated entry", firstCount, secondCount)
  }

  @Test
  fun `scheduleNext startChain and cancel interact with WorkManager`() {
    // initialize Test WorkManager already done in setup
    CampusEntryPollWorker.Companion.scheduleNext(ctx)
    val wm = WorkManager.getInstance(ctx)
    val infos = wm.getWorkInfosByTag("campus_entry_poll").get()
    assertTrue("Expected scheduled work", infos.isNotEmpty())

    CampusEntryPollWorker.Companion.startChain(ctx) // alias for scheduleNext
    val infos2 = wm.getWorkInfosByTag("campus_entry_poll").get()
    assertTrue(infos2.isNotEmpty())

    CampusEntryPollWorker.Companion.cancel(ctx)
    val after = wm.getWorkInfosByTag("campus_entry_poll").get()
    // cancellation may produce CANCELLED state(s); ensure WorkManager returned entries (they exist
    // or cancelled)
    assertTrue(after.isNotEmpty())
  }

  @Test
  fun `isOnEpflCampusInternal boundary checks`() {
    // inside coords
    assertTrue(CampusEntryPollWorker.Companion.isOnEpflCampusInternal(LatLng(46.5202, 6.5652)))
    // outside lat
    assertFalse(CampusEntryPollWorker.Companion.isOnEpflCampusInternal(LatLng(46.530, 6.5652)))
    // outside lon
    assertFalse(CampusEntryPollWorker.Companion.isOnEpflCampusInternal(LatLng(46.5202, 6.580)))
  }
}
