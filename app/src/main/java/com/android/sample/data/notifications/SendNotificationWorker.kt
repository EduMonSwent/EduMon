package com.android.sample.data.notifications

import android.Manifest
import android.app.Notification
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.sample.R

@VisibleForTesting
internal fun buildOneShotNotification(ctx: Context): Notification =
    NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("EduMon")
        .setContentText("⏰ Your scheduled test notification is here!")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

class SendNotificationWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  override suspend fun doWork(): Result {
    NotificationUtils.ensureChannel(applicationContext)
    val n = buildOneShotNotification(applicationContext)
    NotificationManagerCompat.from(applicationContext)
        .notify(NotificationUtils.TEST_NOTIFICATION_ID, n)
    return Result.success()
  }
}
