// app/src/main/java/com/android/sample/data/notifications/NotificationUtils.kt
package com.android.sample.data.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationUtils {
  const val CHANNEL_ID = "edumon_default"

  // IDs stables pour nos notifications
  const val TEST_NOTIFICATION_ID = 1001
  const val ID_KEEP_STREAK = 1002
  const val ID_STUDY_KICKOFF = 1003
  const val ID_CAMPUS_ENTRY = 1004

  fun ensureChannel(ctx: Context) {
    val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
      val channel =
          NotificationChannel(
                  CHANNEL_ID,
                  "EduMon",
                  NotificationManager.IMPORTANCE_HIGH // Changed from DEFAULT to HIGH
                  )
              .apply {
                description = "EduMon notifications including campus entry alerts"
                setShowBadge(true)
                // Allow notifications to show on lock screen (minSdk is 28, so this is always
                // available)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
              }
      mgr.createNotificationChannel(channel)
    }
  }
}
