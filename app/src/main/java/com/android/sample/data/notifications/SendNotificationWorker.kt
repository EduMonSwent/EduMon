package com.android.sample.data.notifications

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.sample.R

class SendNotificationWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  override suspend fun doWork(): Result {
    NotificationUtils.ensureChannel(applicationContext)

    val n =
        NotificationCompat.Builder(applicationContext, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("EduMon")
            .setContentText("⏰ Your scheduled test notification is here!")
            .setAutoCancel(true)
            .build()

    NotificationManagerCompat.from(applicationContext).notify(NotificationUtils.ID_TEST, n)

    return Result.success()
  }
}
