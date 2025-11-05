package com.android.sample.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationUtils {
  const val CHANNEL_ID = "edumon.reminders"
  const val CHANNEL_NAME = "EduMon Reminders"
  const val CHANNEL_DESC = "General reminders and scheduled notifications."

  const val ID_TEST = 1001
  const val ID_STUDY_KICKOFF = 1002
  const val ID_KEEP_STREAK = 1003

  fun ensureChannel(ctx: Context) {
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = CHANNEL_DESC })
  }
}
