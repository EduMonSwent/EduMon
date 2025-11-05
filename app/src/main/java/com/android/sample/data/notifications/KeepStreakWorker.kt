package com.android.sample.data.notifications

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.sample.R

class KeepStreakWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  override suspend fun doWork(): Result {
    NotificationUtils.ensureChannel(applicationContext)

    val days = StreakPrefs.get(applicationContext)
    val msg = buildString {
      append("Don’t let your streak of ")
      append(days)
      append(" day")
      if (days != 1) append("s")
      append(" disappear")
    }

    val n =
        NotificationCompat.Builder(applicationContext, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Keep your streak 🔥")
            .setContentText(msg)
            .setAutoCancel(true)
            .build()

    NotificationManagerCompat.from(applicationContext).notify(NotificationUtils.ID_KEEP_STREAK, n)

    return Result.success()
  }
}
