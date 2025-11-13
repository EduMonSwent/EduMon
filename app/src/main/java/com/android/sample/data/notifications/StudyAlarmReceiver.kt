package com.android.sample.data.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.android.sample.R

/** Receives exact alarms and posts the notification with a deep-link to the study session. */
class StudyAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val eventId = intent.getStringExtra("event_id") ?: return

    // Build deep link pending intent
    val deepLink = "edumon://study_session/$eventId"
    val target =
        Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply { `package` = context.packageName }

    val pi =
        PendingIntent.getActivity(
            context, 0, target, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    // Ensure channel exists
    NotificationUtils.ensureChannel(context)

    // Permission check for POST_NOTIFICATIONS (Android 13+)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      val granted =
          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
              android.content.pm.PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    val n =
        NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.study_alarm_title))
            .setContentText(context.getString(R.string.study_alarm_text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

    NotificationManagerCompat.from(context).notify(eventId.hashCode(), n)
  }
}
