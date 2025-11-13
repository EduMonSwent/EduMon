package com.android.sample.data.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SendNotificationWorkerTest {

  private val ctx: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun builds_and_posts_one_shot() {
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadow = Shadows.shadowOf(nm)
    val before = shadow.allNotifications.size

    NotificationUtils.ensureChannel(ctx)
    val n = buildOneShotNotification(ctx)
    nm.notify(NotificationUtils.TEST_NOTIFICATION_ID, n)

    val after = shadow.allNotifications.size
    assertEquals(before + 1, after)
  }

  @Test
  fun `buildOneShotNotification sets contentIntent when provided`() {
    val intent = Intent(ctx, SendNotificationWorker::class.java)
    val pending =
        PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    val notification = buildOneShotNotification(ctx, pending)
    assertNotNull(notification.contentIntent)
  }

  @Test
  fun `SendNotificationWorker doWork posts notification with deep link contentIntent`() {
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadow = Shadows.shadowOf(nm)
    val before = shadow.allNotifications.size

    val deepLink = "edumon://study_session/test-event"

    val worker =
        TestListenableWorkerBuilder<SendNotificationWorker>(ctx)
            .setInputData(Data.Builder().putString("deep_link", deepLink).build())
            .build()

    val result = worker.startWork().get()

    assertTrue(result is ListenableWorker.Result.Success)

    val after = shadow.allNotifications.size
    assertEquals(before + 1, after)

    val posted = shadow.allNotifications.last()
    val pi = posted.contentIntent
    assertNotNull(pi)

    val savedIntent = Shadows.shadowOf(pi).savedIntent
    assertNotNull(savedIntent)
    assertEquals(Intent.ACTION_VIEW, savedIntent.action)
    assertEquals(Uri.parse(deepLink), savedIntent.data)
    assertEquals(ctx.packageName, savedIntent.`package`)
  }
}
