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

// Parts of this code were written with ChatGPT assistance

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
    WorkManagerTestInitHelper.initializeTestWorkManager(context)

    val app = context as android.app.Application
    val shadowApp: ShadowApplication = shadowOf(app)
    shadowApp.grantPermissions(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION)

    worker =
        TestListenableWorkerBuilder<CampusEntryPollWorker>(context)
            .setInputData(
                androidx.work.Data.Builder()
                    .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
                    .build())
            .build()

    campusPollPrefs = context.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
    locationPrefs = context.getSharedPreferences("last_location", Context.MODE_PRIVATE)

    campusPollPrefs.edit().clear().commit()
    locationPrefs.edit().clear().commit()
  }

  // --- Early exits ---

  @Test
  fun `returns success and no notification when location permission denied`() = runTest {
    val app = context as android.app.Application
    val shadowApp: ShadowApplication = shadowOf(app)
    shadowApp.denyPermissions(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    assertEquals(0, shadowNotificationManager.allNotifications.size)
  }

  @Test
  fun `returns success and no notification when notification permission denied`() = runTest {
    val app = context as android.app.Application
    val shadowApp: ShadowApplication = shadowOf(app)
    shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

    locationPrefs.edit().putFloat("lat", 46.5202f).putFloat("lon", 6.5652f).commit()

    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)
    assertEquals(0, shadowNotificationManager.allNotifications.size)
  }

  // --- Core campus entry logic ---

  @Test
  fun `posts notification only on first campus entry`() = runTest {
    campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()
    locationPrefs.edit().putFloat("lat", 46.5202f).putFloat("lon", 6.5652f).commit()

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)

    // First run: should post
    val first = worker.doWork()
    assertEquals(ListenableWorker.Result.success(), first)
    val firstCount = shadowNotificationManager.allNotifications.size
    assertTrue(firstCount > 0)
    assertTrue(campusPollPrefs.getBoolean("was_on_campus", false))

    // Second run on campus: no extra notification
    val second = worker.doWork()
    assertEquals(ListenableWorker.Result.success(), second)
    val secondCount = shadowNotificationManager.allNotifications.size
    assertEquals(firstCount, secondCount)
  }

  @Test
  fun `off campus location updates state without notification`() = runTest {
    campusPollPrefs.edit().putBoolean("was_on_campus", true).commit()
    locationPrefs.edit().putFloat("lat", 46.510f).putFloat("lon", 6.550f).commit()

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotificationManager = shadowOf(notificationManager)

    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)
    assertFalse(campusPollPrefs.getBoolean("was_on_campus", true))
    assertEquals(0, shadowNotificationManager.allNotifications.size)
  }

  // --- skipFetch and stored location paths ---

  @Test
  fun `skipFetch without coordinates and without stored location returns success with no state change`() =
      runTest {
        campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()
        val testWorker =
            TestListenableWorkerBuilder<CampusEntryPollWorker>(context)
                .setInputData(
                    androidx.work.Data.Builder()
                        .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
                        .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
                        .build())
                .build()

        val result = testWorker.startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
        assertFalse(campusPollPrefs.getBoolean("was_on_campus", true))
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
      }

  @Test
  fun `skipFetch without coordinates uses stored on-campus location and posts notification`() =
      runTest {
        locationPrefs.edit().putFloat("lat", 46.5202f).putFloat("lon", 6.5652f).commit()
        campusPollPrefs.edit().putBoolean("was_on_campus", false).commit()
        val testWorker =
            TestListenableWorkerBuilder<CampusEntryPollWorker>(context)
                .setInputData(
                    androidx.work.Data.Builder()
                        .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
                        .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
                        .build())
                .build()

        val result = testWorker.startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(campusPollPrefs.getBoolean("was_on_campus", false))
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifs = shadowOf(nm).allNotifications
        assertTrue("Notification expected for campus entry", notifs.isNotEmpty())
      }

  @Test
  fun `skipFetch without coordinates uses stored off-campus location and updates state without notification`() =
      runTest {
        locationPrefs.edit().putFloat("lat", 46.510f).putFloat("lon", 6.550f).commit()
        campusPollPrefs.edit().putBoolean("was_on_campus", true).commit()
        val testWorker =
            TestListenableWorkerBuilder<CampusEntryPollWorker>(context)
                .setInputData(
                    androidx.work.Data.Builder()
                        .putBoolean(CampusEntryPollWorker.KEY_DISABLE_CHAIN, true)
                        .putBoolean(CampusEntryPollWorker.KEY_TEST_SKIP_FETCH, true)
                        .build())
                .build()

        val result = testWorker.startWork().get()

        assertEquals(ListenableWorker.Result.success(), result)
        assertFalse(campusPollPrefs.getBoolean("was_on_campus", true))
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue(
            "No notification expected for leaving campus", shadowOf(nm).allNotifications.isEmpty())
      }

  @Test
  fun `test coordinates shortcut marks on-campus and posts notification`() = runTest {
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
    assertTrue(campusPollPrefs.getBoolean("was_on_campus", false))
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertTrue(shadowOf(nm).allNotifications.isNotEmpty())
  }

  // --- WorkManager chaining helpers ---

  @Test
  fun `scheduleNext enqueues unique work with expected tag`() = runTest {
    CampusEntryPollWorker.scheduleNext(context)

    val wm = androidx.work.WorkManager.getInstance(context)
    val infos = wm.getWorkInfosByTag("campus_entry_poll").get()
    assertTrue("Expected next poll to be scheduled", infos.isNotEmpty())
  }

  @Test
  fun `startChain delegates to scheduleNext`() = runTest {
    CampusEntryPollWorker.startChain(context)

    val wm = androidx.work.WorkManager.getInstance(context)
    val infos = wm.getWorkInfosByTag("campus_entry_poll").get()
    assertTrue(infos.isNotEmpty())
  }

  @Test
  fun `cancel stops the scheduled unique work`() = runTest {
    CampusEntryPollWorker.startChain(context)

    val wm = androidx.work.WorkManager.getInstance(context)
    val before = wm.getWorkInfosByTag("campus_entry_poll").get()
    assertTrue(before.isNotEmpty())

    CampusEntryPollWorker.cancel(context)

    val after = wm.getWorkInfosByTag("campus_entry_poll").get()
    assertTrue(
        "Expected all scheduled work to be cancelled",
        after.isNotEmpty() && after.all { it.state == androidx.work.WorkInfo.State.CANCELLED })
  }
}
