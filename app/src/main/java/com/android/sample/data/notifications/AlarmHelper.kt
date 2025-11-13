package com.android.sample.data.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.android.sample.R
import java.util.concurrent.TimeUnit

/**
 * Helper to schedule and cancel exact alarms for study session reminders. Uses a deterministic
 * PendingIntent based on the eventId to allow cancellation.
 */
object AlarmHelper {

  private const val ALARM_ACTION = "com.android.sample.ACTION_STUDY_ALARM"

  @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
  fun scheduleStudyAlarm(context: Context, eventId: String, triggerAtMillis: Long) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = buildPendingIntent(context, eventId)

    // Cancel any previous identical intent first
    am.cancel(pi)

    // Clamp to future to avoid scheduling in the past
    val now = System.currentTimeMillis()
    val millis = if (triggerAtMillis <= now) now + 1000L else triggerAtMillis

    // Try exact alarm first; if not allowed, fallback to WorkManager
    try {
      am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
      return
    } catch (se: SecurityException) {
      Log.w("AlarmHelper", "Exact alarm not permitted, falling back to WorkManager", se)
    } catch (iae: IllegalArgumentException) {
      Log.w("AlarmHelper", "Failed to set exact alarm, falling back to WorkManager", iae)
    } catch (t: Throwable) {
      Log.w("AlarmHelper", "Unexpected failure setting exact alarm, falling back", t)
    }

    // Fallback: schedule using WorkManager with equivalent delay
    try {
      val delay = millis - System.currentTimeMillis()
      val deepLink = context.getString(R.string.deep_link_format, eventId)
      val data =
          Data.Builder().putString("deep_link", deepLink).putString("event_id", eventId).build()

      val req =
          OneTimeWorkRequestBuilder<SendNotificationWorker>()
              .setInitialDelay(if (delay > 0) delay else 0, TimeUnit.MILLISECONDS)
              .setInputData(data)
              .addTag("study_session_start")
              .build()

      WorkManager.getInstance(context)
          .enqueueUniqueWork("study_session_start_$eventId", ExistingWorkPolicy.REPLACE, req)
    } catch (t: Throwable) {
      Log.e("AlarmHelper", "Failed to schedule fallback WorkManager alarm for $eventId", t)
    }
  }

  fun cancelStudyAlarm(context: Context, eventId: String) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = buildPendingIntent(context, eventId)
    try {
      am.cancel(pi)
    } catch (t: Throwable) {
      Log.w("AlarmHelper", "Failed to cancel alarm via AlarmManager", t)
    }
    // Also cancel any WorkManager fallback
    try {
      WorkManager.getInstance(context).cancelUniqueWork("study_session_start_$eventId")
    } catch (_: Throwable) {
      Log.w("AlarmHelper", "Failed to cancel fallback via WorkManager")
    }
  }

  private fun buildPendingIntent(context: Context, eventId: String): PendingIntent {
    val intent =
        Intent(context, StudyAlarmReceiver::class.java).apply {
          action = ALARM_ACTION
          putExtra("event_id", eventId)
        }
    val requestCode = eventId.hashCode()
    return PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  }
}
