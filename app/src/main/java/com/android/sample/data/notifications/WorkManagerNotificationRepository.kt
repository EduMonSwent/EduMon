package com.android.sample.data.notifications

import android.content.Context
import androidx.work.*
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WorkManagerNotificationRepository(
    private val schedulerProvider: (Context) -> WorkScheduler = { WorkManagerScheduler(it) }
) : NotificationRepository {

  override fun scheduleOneMinuteFromNow(context: Context) {
    val req =
        OneTimeWorkRequestBuilder<SendNotificationWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
    schedulerProvider(context).enqueueUniqueWork("one_min_test", ExistingWorkPolicy.REPLACE, req)
  }

  override fun setDailyEnabled(
      context: Context,
      kind: NotificationKind,
      enabled: Boolean,
      hour24: Int
  ) {
    val name =
        when (kind) {
          NotificationKind.NO_WORK_TODAY -> "daily_kickoff_ALL"
          NotificationKind.KEEP_STREAK -> "daily_streak_ALL"
        }
    val scheduler = schedulerProvider(context)
    if (!enabled) {
      scheduler.cancelUniqueWork(name)
      return
    }
    val delay = nextDelayMillisToHour(hour24, 0)
    val req =
        when (kind) {
              NotificationKind.NO_WORK_TODAY ->
                  PeriodicWorkRequestBuilder<StudyKickoffWorker>(24, TimeUnit.HOURS)
              NotificationKind.KEEP_STREAK ->
                  PeriodicWorkRequestBuilder<KeepStreakWorker>(24, TimeUnit.HOURS)
            }
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(name)
            .build()

    scheduler.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, req)
  }

  override fun setWeeklySchedule(
      context: Context,
      kind: NotificationKind,
      enabled: Boolean,
      times: Map<Int, Pair<Int, Int>>
  ) {
    val scheduler = schedulerProvider(context)
    val prefix =
        when (kind) {
          NotificationKind.NO_WORK_TODAY -> "kickoff"
          NotificationKind.KEEP_STREAK -> "streak"
        }

    // Clear previous weekly works
    listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY)
        .forEach { scheduler.cancelUniqueWork("${prefix}_$it") }

    if (!enabled) return

    times.forEach { (day, hm) ->
      val (h, m) = hm
      val delay = nextDelayMillisToDayTime(day, h, m)
      val req =
          when (kind) {
                NotificationKind.NO_WORK_TODAY ->
                    PeriodicWorkRequestBuilder<StudyKickoffWorker>(7, TimeUnit.DAYS)
                NotificationKind.KEEP_STREAK ->
                    PeriodicWorkRequestBuilder<KeepStreakWorker>(7, TimeUnit.DAYS)
              }
              .setInitialDelay(delay, TimeUnit.MILLISECONDS)
              .addTag("${prefix}_$day")
              .build()

      scheduler.enqueueUniquePeriodicWork("${prefix}_$day", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
  }

  override fun cancel(context: Context, kind: NotificationKind) {
    val scheduler = schedulerProvider(context)
    val prefix =
        when (kind) {
          NotificationKind.NO_WORK_TODAY -> "kickoff"
          NotificationKind.KEEP_STREAK -> "streak"
        }
    listOf(
            "${prefix}_${Calendar.MONDAY}",
            "${prefix}_${Calendar.TUESDAY}",
            "${prefix}_${Calendar.WEDNESDAY}",
            "${prefix}_${Calendar.THURSDAY}",
            "${prefix}_${Calendar.FRIDAY}",
            "${prefix}_${Calendar.SATURDAY}",
            "${prefix}_${Calendar.SUNDAY}",
            if (kind == NotificationKind.NO_WORK_TODAY) "daily_kickoff_ALL" else "daily_streak_ALL")
        .forEach { scheduler.cancelUniqueWork(it) }
  }

  // --- helpers ---
  private fun nextDelayMillisToHour(hour24: Int, minute: Int): Long {
    val now = Calendar.getInstance()
    val next =
        (now.clone() as Calendar).apply {
          set(Calendar.HOUR_OF_DAY, hour24.coerceIn(0, 23))
          set(Calendar.MINUTE, minute.coerceIn(0, 59))
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
          if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
    return next.timeInMillis - now.timeInMillis
  }

  private fun nextDelayMillisToDayTime(dayOfWeek: Int, hour24: Int, minute: Int): Long {
    val now = Calendar.getInstance()
    val next =
        (now.clone() as Calendar).apply {
          set(Calendar.DAY_OF_WEEK, dayOfWeek)
          set(Calendar.HOUR_OF_DAY, hour24.coerceIn(0, 23))
          set(Calendar.MINUTE, minute.coerceIn(0, 59))
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
          while (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 7)
        }
    return next.timeInMillis - now.timeInMillis
  }
}
