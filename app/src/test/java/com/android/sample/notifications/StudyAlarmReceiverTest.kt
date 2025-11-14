package com.android.sample.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.android.sample.data.notifications.StudyAlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Android 13 (Tiramisu)
class StudyAlarmReceiverTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun `does not post notification when POST_NOTIFICATIONS is denied on Tiramisu`() {
    val app: Application = ApplicationProvider.getApplicationContext()
    val shadowApp = Shadows.shadowOf(app)
    shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

    val receiver = StudyAlarmReceiver()
    val intent = Intent().apply { putExtra("event_id", "evt-denied") }

    receiver.onReceive(context, intent)

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNm = Shadows.shadowOf(nm)

    assertTrue(
        "Une notification a été postée alors que la permission est refusée",
        shadowNm.allNotifications.isEmpty())
  }

  @Test
  fun `posts notification when POST_NOTIFICATIONS is granted on Tiramisu`() {
    val app: Application = ApplicationProvider.getApplicationContext()
    val shadowApp = Shadows.shadowOf(app)
    shadowApp.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    val receiver = StudyAlarmReceiver()
    val intent = Intent().apply { putExtra("event_id", "evt-granted") }

    receiver.onReceive(context, intent)

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNm = Shadows.shadowOf(nm)

    val count = shadowNm.allNotifications.size
    assertEquals("La notification aurait dû être postée", 1, count)
  }
}
