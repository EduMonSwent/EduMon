package com.android.sample.data.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.sample.R

@VisibleForTesting
internal fun buildOneShotNotification(
    ctx: Context,
    contentIntent: PendingIntent? = null
): Notification {
  val builder =
      NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID)
          .setSmallIcon(R.mipmap.ic_launcher)
          .setContentTitle("EduMon")
          .setContentText("⏰ Your scheduled test notification is here!")
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setAutoCancel(true)
          .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen

  if (contentIntent != null) {
    builder.setContentIntent(contentIntent)
  }

  return builder.build()
}

class SendNotificationWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

  @RequiresPermission(value = "android.permission.POST_NOTIFICATIONS", conditional = true)
  override suspend fun doWork(): Result {
    NotificationUtils.ensureChannel(applicationContext)

    // Try to read a deep link passed through inputData
    val deepLink = inputData.getString("deep_link")
    val pending: PendingIntent? =
        if (!deepLink.isNullOrEmpty()) {
          try {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                  // restrict to our app
                  `package` = applicationContext.packageName
                }
            PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
          } catch (_: Exception) {
            null
          }
        } else null

    val n = buildOneShotNotification(applicationContext, pending)
    NotificationManagerCompat.from(applicationContext)
        .notify(NotificationUtils.TEST_NOTIFICATION_ID, n)
    return Result.success()
  }
}
