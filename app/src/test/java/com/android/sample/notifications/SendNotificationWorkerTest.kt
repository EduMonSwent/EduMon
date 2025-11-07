package com.android.sample.data.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
}
