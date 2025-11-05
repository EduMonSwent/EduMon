package com.android.sample.model.notifications

import android.content.Context

enum class NotificationKind {
  NO_WORK_TODAY,
  KEEP_STREAK
} // NO_WORK_TODAY = "Study Kickoff"

/** Scheduling contract (WorkManager-backed). */
interface NotificationRepository {
  /** One-shot ~1 minute later (best-effort). */
  fun scheduleOneMinuteFromNow(context: Context)

  /** Simple daily toggle (kept for compatibility). */
  fun setDailyEnabled(context: Context, kind: NotificationKind, enabled: Boolean, hour24: Int = 20)

  /**
   * Weekly schedule: a time for each weekday. Key = Calendar.MONDAY..SUNDAY ; Value = Pair(hour24,
   * minute)
   */
  fun setWeeklySchedule(
      context: Context,
      kind: NotificationKind,
      enabled: Boolean,
      times: Map<Int, Pair<Int, Int>>
  )

  /** Cancel all works for that kind. */
  fun cancel(context: Context, kind: NotificationKind)
}
