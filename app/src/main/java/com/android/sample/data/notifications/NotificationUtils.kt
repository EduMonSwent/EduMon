// app/src/main/java/com/android/sample/data/notifications/NotificationUtils.kt
package com.android.sample.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationUtils {
  const val CHANNEL_ID = "edumon_default"

  // IDs stables pour nos notifications
  const val TEST_NOTIFICATION_ID = 1001
  const val ID_KEEP_STREAK = 1002
  const val ID_STUDY_KICKOFF = 1003 // ✅ ajoute celui-ci

  fun ensureChannel(ctx: Context) {
    val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
      mgr.createNotificationChannel(
          NotificationChannel(CHANNEL_ID, "EduMon", NotificationManager.IMPORTANCE_DEFAULT))
    }
  }
}
