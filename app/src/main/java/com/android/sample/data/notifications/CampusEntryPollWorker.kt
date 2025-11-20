package com.android.sample.data.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.android.sample.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

// Parts of this code were written with ChatGPT assistance

/**
 * Polling worker to detect campus entry at ~5 minute intervals. Because PeriodicWorkRequest
 * enforces 15m minimum interval, we chain OneTimeWorkRequests manually with 5 minute delays.
 *
 * IMPORTANT: This worker actively fetches the device's current location in the background using
 * FusedLocationProviderClient. For best results on Android 10+, users should grant
 * ACCESS_BACKGROUND_LOCATION permission (though it will fallback to last known location if
 * background location is denied).
 */
class CampusEntryPollWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    // Reschedule first (fire and forget) to sustain the chain even if current run fails later.
    // Allow tests to disable the chaining via input data flag.
    val disableChain = inputData.getBoolean(KEY_DISABLE_CHAIN, false)
    if (!disableChain) {
      scheduleNext(applicationContext)
    }

    // Permissions: need fine or coarse location; background recommended for reliability.
    val fineGranted =
        ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    val coarseGranted =
        ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    if (!(fineGranted || coarseGranted)) {
      Log.w(TAG, "Location permission not granted, skipping campus detection")
      return Result.success()
    }

    // POST_NOTIFICATIONS (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val notifGranted =
          ContextCompat.checkSelfPermission(
              applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
              PackageManager.PERMISSION_GRANTED
      if (!notifGranted) {
        Log.w(TAG, "Notification permission not granted, skipping campus detection")
        return Result.success()
      }
    }

    // ---------- Test shortcuts & Robolectric fallback ----------
    val skipFetch = inputData.getBoolean(KEY_TEST_SKIP_FETCH, false)
    val testLat = inputData.getDouble(KEY_TEST_LAT, Double.NaN)
    val testLon = inputData.getDouble(KEY_TEST_LON, Double.NaN)

    val position: LatLng =
        when {
          // Explicit test coordinates provided
          skipFetch && !testLat.isNaN() && !testLon.isNaN() -> LatLng(testLat, testLon)
          // Skip fetch and try prefs or default center
          skipFetch -> {
            val locPrefs =
                applicationContext.getSharedPreferences("last_location", Context.MODE_PRIVATE)
            val hasStored = locPrefs.contains("lat") && locPrefs.contains("lon")
            if (!hasStored) {
              Log.w(TAG, "Skip-fetch without stored location; skipping campus detection")
              return Result.success()
            }
            val lat = locPrefs.getFloat("lat", 46.5202f).toDouble()
            val lon = locPrefs.getFloat("lon", 6.5652f).toDouble()
            LatLng(lat, lon)
          }
          // Robolectric environment: avoid FusedLocationProviderClient which may hang
          Build.FINGERPRINT.startsWith("robolectric") -> {
            val locPrefs =
                applicationContext.getSharedPreferences("last_location", Context.MODE_PRIVATE)
            val hasStored = locPrefs.contains("lat") && locPrefs.contains("lon")
            if (!hasStored) {
              Log.w(TAG, "Robolectric without stored location; skipping campus detection")
              return Result.success()
            }
            val lat = locPrefs.getFloat("lat", 46.5202f).toDouble()
            val lon = locPrefs.getFloat("lon", 6.5652f).toDouble()
            LatLng(lat, lon)
          }
          // Normal production path: active fetch with fallback
          else -> {
            try {
              getCurrentLocation(applicationContext)
            } catch (e: Exception) {
              Log.w(TAG, "Failed to get current location: ${e.message}")
              val locPrefs =
                  applicationContext.getSharedPreferences("last_location", Context.MODE_PRIVATE)
              if (locPrefs.contains("lat") && locPrefs.contains("lon")) {
                val lat = locPrefs.getFloat("lat", 46.5200f).toDouble()
                val lon = locPrefs.getFloat("lon", 6.5650f).toDouble()
                LatLng(lat, lon)
              } else {
                Log.w(TAG, "No location available, skipping campus detection")
                return Result.success()
              }
            }
          }
        }

    // Store the fetched / chosen location for other components to use
    applicationContext
        .getSharedPreferences("last_location", Context.MODE_PRIVATE)
        .edit()
        .putFloat("lat", position.latitude.toFloat())
        .putFloat("lon", position.longitude.toFloat())
        .apply()

    val onCampus = isOnEpflCampus(position)

    // Simple dedupe: read last flag stored in DataStore/SharedPreferences (lightweight here using a
    // static key)
    val prefs = applicationContext.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
    val wasOnCampus = prefs.getBoolean(KEY_WAS_ON_CAMPUS, false)

    if (onCampus && !wasOnCampus) {
      // Fire notification if permission granted
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
          ContextCompat.checkSelfPermission(
              applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
              PackageManager.PERMISSION_GRANTED) {
        postCampusEntryNotification(applicationContext)
      }
    }

    // Persist current state
    prefs.edit().putBoolean(KEY_WAS_ON_CAMPUS, onCampus).apply()

    return Result.success()
  }

  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  private fun postCampusEntryNotification(ctx: Context) {
    NotificationUtils.ensureChannel(ctx)

    // Removed deep link: we no longer attach a content intent so tapping does nothing.
    val n =
        NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(ctx.getString(R.string.campus_entry_title))
            .setContentText(ctx.getString(R.string.campus_entry_text))
            // .setContentIntent(pi) // removed
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    NotificationManagerCompat.from(ctx).notify(CAMPUS_ENTRY_NOTIFICATION_ID, n)
  }

  /**
   * Fetch current device location using FusedLocationProviderClient. This requires
   * ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permission. For background use,
   * ACCESS_BACKGROUND_LOCATION is also recommended (API 29+).
   */
  @Suppress("MissingPermission") // Permission already checked in doWork()
  private suspend fun getCurrentLocation(context: Context): LatLng {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Try to get current location with a timeout
    val cancellationTokenSource = CancellationTokenSource()
    val location =
        fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
            .await()

    return if (location != null) {
      LatLng(location.latitude, location.longitude)
    } else {
      // Fallback to last known location if current location fails
      val lastLocation = fusedLocationClient.lastLocation.await()
      if (lastLocation != null) {
        LatLng(lastLocation.latitude, lastLocation.longitude)
      } else {
        throw IllegalStateException("No location available")
      }
    }
  }

  private fun isOnEpflCampus(position: LatLng): Boolean {
    val lat = position.latitude
    val lng = position.longitude
    val minLat = 46.515
    val maxLat = 46.525
    val minLng = 6.555
    val maxLng = 6.575
    return lat in minLat..maxLat && lng in minLng..maxLng
  }

  companion object {
    private const val TAG = "CampusEntryPollWorker"
    private const val KEY_WAS_ON_CAMPUS = "was_on_campus"
    private const val CAMPUS_ENTRY_NOTIFICATION_ID = 9101
    private const val UNIQUE_WORK_NAME = "campus_entry_poll"
    private const val DELAY_MINUTES = 5L
    internal const val KEY_DISABLE_CHAIN = "disable_chain" // for tests only
    // New test-only keys
    internal const val KEY_TEST_SKIP_FETCH = "test_skip_fetch"
    internal const val KEY_TEST_LAT = "test_lat"
    internal const val KEY_TEST_LON = "test_lon"

    fun scheduleNext(ctx: Context) {
      val request =
          OneTimeWorkRequestBuilder<CampusEntryPollWorker>()
              .setInitialDelay(DELAY_MINUTES, TimeUnit.MINUTES)
              .addTag(UNIQUE_WORK_NAME)
              .build()
      WorkManager.getInstance(ctx)
          .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun startChain(ctx: Context) = scheduleNext(ctx)

    fun cancel(ctx: Context) {
      WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
  }
}
