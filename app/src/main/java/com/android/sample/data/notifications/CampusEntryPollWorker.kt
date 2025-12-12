package com.android.sample.data.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
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
 * Polling worker to detect campus presence at ~5 minute intervals. Because PeriodicWorkRequest
 * enforces 15m minimum interval, we chain OneTimeWorkRequests manually with 5 minute delays.
 *
 * This worker tracks whether the user is on campus by periodically checking their location and
 * updating the campus state in SharedPreferences for other components to use.
 *
 * IMPORTANT: This worker actively fetches the device's current location in the background using
 * FusedLocationProviderClient. For best results on Android 10+, users should grant
 * ACCESS_BACKGROUND_LOCATION permission (though it will fallback to last known location if
 * background location is denied).
 */
class CampusEntryPollWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    // Sustain chain (unless disabled by tests)
    if (!inputData.getBoolean(KEY_DISABLE_CHAIN, false)) scheduleNext(applicationContext)

    // Guard: location permission
    if (!hasLocationPermission(applicationContext)) {
      Log.w(TAG, "Location permission not granted, skipping campus detection")
      return Result.success()
    }

    // Resolve a position (test/robolectric/normal)
    val skipFetch = inputData.getBoolean(KEY_TEST_SKIP_FETCH, false)
    val testLat = inputData.getDouble(KEY_TEST_LAT, Double.NaN)
    val testLon = inputData.getDouble(KEY_TEST_LON, Double.NaN)

    val position =
        resolvePosition(applicationContext, skipFetch, testLat, testLon)
            ?: return Result.success() // nothing to do

    // Persist last location for other components and future fallbacks
    persistLastLocation(applicationContext, position)

    val onCampus = isOnEpflCampus(position)

    // Check if we should send a notification (transitioned from OFF → ON campus)
    if (shouldPostCampusEntryNotification(applicationContext, onCampus)) {
      // Send notification if permission is granted (API 33+) or pre-Tiramisu
      if (hasNotificationPermission(applicationContext)) {
        try {
          postCampusEntryNotification(applicationContext)
        } catch (se: SecurityException) {
          Log.w(TAG, "Notification post aborted due to missing permission at runtime", se)
        }
      } else {
        Log.w(TAG, "Notification permission not granted, skipping campus entry notification")
      }
    }

    // Persist new state for next run (other components can check this)
    updateCampusFlag(applicationContext, onCampus)

    return Result.success()
  }

  /* ---------------------------- helpers: permissions ---------------------------- */

  private fun hasLocationPermission(ctx: Context): Boolean {
    val fine =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    val coarse =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  private fun hasNotificationPermission(ctx: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
    } else {
      true // Pre-Tiramisu doesn't require runtime permission
    }
  }

  /* ---------------------------- helpers: location IO ---------------------------- */

  private fun readStoredLocation(ctx: Context): LatLng? {
    val sp = ctx.getSharedPreferences("last_location", Context.MODE_PRIVATE)
    if (!sp.contains("lat") || !sp.contains("lon")) return null
    val lat = sp.getFloat("lat", DEFAULT_LAT).toDouble()
    val lon = sp.getFloat("lon", DEFAULT_LON).toDouble()
    return LatLng(lat, lon)
  }

  private fun persistLastLocation(ctx: Context, pos: LatLng) {
    ctx.getSharedPreferences("last_location", Context.MODE_PRIVATE).edit(commit = true) {
      putFloat("lat", pos.latitude.toFloat()).putFloat("lon", pos.longitude.toFloat())
    } // Use commit() for synchronous write, ensuring data is available immediately
  }

  private suspend fun resolvePosition(
      ctx: Context,
      skipFetch: Boolean,
      testLat: Double,
      testLon: Double
  ): LatLng? {
    testCoordinatesOrNull(skipFetch, testLat, testLon)?.let {
      return it
    }
    if (skipFetch || Build.FINGERPRINT.startsWith("robolectric")) {
      return storedLocationOrWarn(
          ctx, "Skip-fetch/Robolectric without stored location; skipping campus detection")
    }
    return activeFetchOrStored(ctx)
  }
  // Return explicit test coordinates if both provided and skipFetch requested
  private fun testCoordinatesOrNull(skipFetch: Boolean, lat: Double, lon: Double): LatLng? {
    return if (skipFetch && !lat.isNaN() && !lon.isNaN()) LatLng(lat, lon) else null
  }
  // Try stored location else log and return null
  private fun storedLocationOrWarn(ctx: Context, warnMsg: String): LatLng? {
    val stored = readStoredLocation(ctx)
    if (stored == null) Log.w(TAG, warnMsg)
    return stored
  }
  // Production path: attempt active fetch; fallback to stored; else warn
  private suspend fun activeFetchOrStored(ctx: Context): LatLng? {
    return try {
      getCurrentLocation(ctx)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to get current location: ${e.message}")
      readStoredLocation(ctx)
          ?: run {
            Log.w(TAG, "No location available, skipping campus detection")
            null
          }
    }
  }

  /**
   * Fetch current device location using FusedLocationProviderClient. This requires
   * ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permission. For background use
   */
  @Suppress("MissingPermission") // Permission already checked in doWork()
  private suspend fun getCurrentLocation(context: Context): LatLng {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    val cancellationTokenSource = CancellationTokenSource()
    val location =
        fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
            .await()

    return if (location != null) {
      LatLng(location.latitude, location.longitude)
    } else {
      val lastLocation = fusedLocationClient.lastLocation.await()
      if (lastLocation != null) {
        LatLng(lastLocation.latitude, lastLocation.longitude)
      } else {
        error("No location available")
      }
    }
  }

  /* ---------------------------- helpers: state & gating ---------------------------- */

  /**
   * Determines if we should post a campus entry notification. Only returns true when transitioning
   * from OFF campus → ON campus.
   */
  private fun shouldPostCampusEntryNotification(ctx: Context, onCampus: Boolean): Boolean {
    val prefs = ctx.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE)
    val wasOnCampus = prefs.getBoolean(KEY_WAS_ON_CAMPUS, false)
    // Notify only when entering campus (transition from false → true)
    return onCampus && !wasOnCampus
  }

  private fun updateCampusFlag(ctx: Context, onCampus: Boolean) {
    ctx.getSharedPreferences("campus_entry_poll", Context.MODE_PRIVATE).edit {
      putBoolean(KEY_WAS_ON_CAMPUS, onCampus)
    }
  }

  /**
   * Post a notification informing the user they have entered campus. Requires POST_NOTIFICATIONS
   * permission on Android 13+.
   */
  @RequiresPermission(value = "android.permission.POST_NOTIFICATIONS", conditional = true)
  private fun postCampusEntryNotification(ctx: Context) {
    NotificationUtils.ensureChannel(ctx)

    val notification =
        NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(ctx.getString(R.string.campus_entry_title))
            .setContentText(ctx.getString(R.string.campus_entry_text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    NotificationManagerCompat.from(ctx).notify(NotificationUtils.ID_CAMPUS_ENTRY, notification)
  }

  private fun isOnEpflCampus(position: LatLng): Boolean = isOnEpflCampusInternal(position)

  companion object {
    private const val TAG = "CampusEntryPollWorker"
    // Default campus center fallback coordinates (EPFL Lausanne approximate)
    private const val DEFAULT_LAT = 46.5202f
    private const val DEFAULT_LON = 6.5652f
    private const val KEY_WAS_ON_CAMPUS = "was_on_campus"
    private const val UNIQUE_WORK_NAME = "campus_entry_poll"
    private const val DELAY_MINUTES = 5L
    internal const val KEY_DISABLE_CHAIN = "disable_chain" // for tests only
    // New test-only keys
    internal const val KEY_TEST_SKIP_FETCH = "test_skip_fetch"
    internal const val KEY_TEST_LAT = "test_lat"
    internal const val KEY_TEST_LON = "test_lon"

    @VisibleForTesting
    internal fun isOnEpflCampusInternal(position: LatLng): Boolean {
      val lat = position.latitude
      val lng = position.longitude
      val minLat = 46.515
      val maxLat = 46.525
      val minLng = 6.555
      val maxLng = 6.575
      return lat in minLat..maxLat && lng in minLng..maxLng
    }

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
