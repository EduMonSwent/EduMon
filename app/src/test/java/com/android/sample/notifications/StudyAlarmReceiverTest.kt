package com.android.sample.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.android.sample.data.notifications.NotificationUtils
import com.android.sample.data.notifications.StudyAlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class StudyAlarmReceiverTest {

  private val ctx: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun onReceive_posts_study_alarm_notification() {
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadow = Shadows.shadowOf(nm)
    val before = shadow.allNotifications.size

    NotificationUtils.ensureChannel(ctx)

    val receiver = StudyAlarmReceiver()
    val intent = Intent().apply { putExtra("event_id", "test-event") }
    receiver.onReceive(ctx, intent)

    val after = shadow.allNotifications.size
    assertEquals(before + 1, after)

    // Assert notification was posted (content inspection is fragile across robolectric versions)
    // Content checks can be added with a specific shadow API if required.
  }
}
