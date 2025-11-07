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
class KeepStreakWorkerTest {

  private val ctx: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun message_pluralization_is_correct() {
    assertEquals("Don’t let your streak of 1 day disappear", buildKeepStreakMessage(1))
    assertEquals("Don’t let your streak of 5 days disappear", buildKeepStreakMessage(5))
  }

  @Test
  fun build_and_post_notification_posts_one() {
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadow = Shadows.shadowOf(nm)
    val before = shadow.allNotifications.size

    NotificationUtils.ensureChannel(ctx)
    val n = buildKeepStreakNotification(ctx, 3)
    postNotification(ctx, NotificationUtils.ID_KEEP_STREAK, n)

    val after = shadow.allNotifications.size
    assertEquals(before + 1, after)
  }
}
