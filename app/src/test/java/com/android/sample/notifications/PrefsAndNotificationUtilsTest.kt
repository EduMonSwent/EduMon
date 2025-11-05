package com.android.sample.notifications

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.android.sample.data.notifications.NotificationUtils
import com.android.sample.data.notifications.StreakPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PrefsAndNotificationUtilsTest {

  @Test
  fun `streak prefs set-get with clamp`() {
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    StreakPrefs.set(ctx, 7)
    assertEquals(7, StreakPrefs.get(ctx))
    StreakPrefs.set(ctx, -2)
    assertEquals(0, StreakPrefs.get(ctx))
  }

  @Test
  fun `ensureChannel creates channel`() {
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    NotificationUtils.ensureChannel(ctx)
    val nm = ctx.getSystemService(NotificationManager::class.java)
    assertNotNull(nm.getNotificationChannel(NotificationUtils.CHANNEL_ID))
  }
}
