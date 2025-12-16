package com.android.sample.data.notifications

import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.sample.R

/** Prompt to start your first study block of the day. */
class StudyKickoffWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
  @RequiresPermission(value = "android.permission.POST_NOTIFICATIONS", conditional = true)
  override suspend fun doWork(): Result {
    NotificationUtils.ensureChannel(applicationContext)
    val n =
        NotificationCompat.Builder(applicationContext, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Study kickoff")
            .setContentText("Time to start your first study block 📚")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .build()
    NotificationManagerCompat.from(applicationContext).notify(NotificationUtils.ID_STUDY_KICKOFF, n)
    return Result.success()
  }
}
