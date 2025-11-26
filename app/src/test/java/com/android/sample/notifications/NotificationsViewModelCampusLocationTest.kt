package com.android.sample.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.android.sample.ui.notifications.NotificationsViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Additional coverage for campus entry enable logic:
 * - Preference persistence already covered in NotificationsViewModelCampusTest
 * - Location fetch and SharedPreferences storage branches
 * - Worker chain invocation covered indirectly (we assert that enabling does not throw)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NotificationsViewModelCampusLocationTest {
  private lateinit var context: Context
  private lateinit var viewModel: NotificationsViewModel

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    // Initialize WorkManager for tests because setCampusEntryEnabled() starts/cancels the chain
    WorkManagerTestInitHelper.initializeTestWorkManager(context)
    viewModel = NotificationsViewModel()
  }

  private fun grantForegroundLocationPermissions(grant: Boolean) {
    mockkStatic("androidx.core.content.ContextCompat")
    every {
      androidx.core.content.ContextCompat.checkSelfPermission(
          context, Manifest.permission.ACCESS_FINE_LOCATION)
    } returns (if (grant) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED)
    every {
      androidx.core.content.ContextCompat.checkSelfPermission(
          context, Manifest.permission.ACCESS_COARSE_LOCATION)
    } returns (if (grant) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED)
  }

  private fun mockLastLocation(location: Location?) {
    mockkStatic(LocationServices::class)
    val fused = mockk<FusedLocationProviderClient>()
    every { fused.lastLocation } returns Tasks.forResult(location)
    every { LocationServices.getFusedLocationProviderClient(context) } returns fused
  }

  @Test
  fun enabling_with_permissions_and_location_stores_last_location() = runBlocking {
    // Grant permissions + provide location
    grantForegroundLocationPermissions(true)
    val loc =
        Location("test").apply {
          latitude = 46.52
          longitude = 6.57
        }
    mockLastLocation(loc)

    viewModel.setCampusEntryEnabled(context, true)

    // Wait briefly for background coroutine to store prefs
    repeat(50) {
      val prefs = context.getSharedPreferences("last_location", Context.MODE_PRIVATE)
      if (prefs.contains("lat") && prefs.contains("lon")) return@repeat
      Thread.sleep(20)
    }

    val prefs = context.getSharedPreferences("last_location", Context.MODE_PRIVATE)
    assertTrue("lat should be stored", prefs.contains("lat"))
    assertTrue("lon should be stored", prefs.contains("lon"))
    val storedLat = prefs.getFloat("lat", Float.NaN)
    val storedLon = prefs.getFloat("lon", Float.NaN)
    assertEquals(46.52f, storedLat, 0.0001f)
    assertEquals(6.57f, storedLon, 0.0001f)
  }

  @Test
  fun enabling_without_permissions_does_not_store_last_location() = runBlocking {
    grantForegroundLocationPermissions(false)
    // Provide a location but permissions denied should short-circuit before using it
    val loc =
        Location("test").apply {
          latitude = 0.0
          longitude = 0.0
        }
    mockLastLocation(loc)

    viewModel.setCampusEntryEnabled(context, true)
    // Wait small window for potential incorrect storage
    Thread.sleep(150)

    val prefs = context.getSharedPreferences("last_location", Context.MODE_PRIVATE)
    assertFalse("lat should NOT be stored when permissions missing", prefs.contains("lat"))
    assertFalse("lon should NOT be stored when permissions missing", prefs.contains("lon"))
  }

  @Test
  fun enabling_with_permissions_but_null_lastLocation_logs_warning_and_no_store() = runBlocking {
    grantForegroundLocationPermissions(true)
    mockLastLocation(null) // Simulate no last known location available

    viewModel.setCampusEntryEnabled(context, true)
    // Wait for coroutine
    Thread.sleep(150)
    val prefs = context.getSharedPreferences("last_location", Context.MODE_PRIVATE)
    assertFalse("lat should not be stored when lastLocation null", prefs.contains("lat"))
    assertFalse("lon should not be stored when lastLocation null", prefs.contains("lon"))
  }

  @Test
  fun disabling_cancels_without_throwing() = runBlocking {
    // Simple branch: ensure disabling after enabling does not throw and state flips
    grantForegroundLocationPermissions(false)
    mockLastLocation(null)
    viewModel.setCampusEntryEnabled(context, true)
    assertTrue(viewModel.campusEntryEnabled.value)
    viewModel.setCampusEntryEnabled(context, false)
    assertFalse(viewModel.campusEntryEnabled.value)
  }
}
