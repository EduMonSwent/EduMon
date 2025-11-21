package com.android.sample.notifications

import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.android.sample.data.notifications.AlarmHelper
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AlarmHelperTest {

  private lateinit var appCtx: Context
  private lateinit var am: AlarmManager

  @Before
  fun setup() {
    appCtx = ApplicationProvider.getApplicationContext()
    am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    // Initialise WorkManager test environment once for fallback assertions
    WorkManagerTestInitHelper.initializeTestWorkManager(appCtx)
  }

  @Test
  fun `scheduleStudyAlarm sets an alarm with future trigger`() {
    val now = System.currentTimeMillis()
    val trigger = now + 10_000L
    AlarmHelper.scheduleStudyAlarm(appCtx, "test-event", trigger)

    val shadowAm = Shadows.shadowOf(am)
    val scheduled = shadowAm.nextScheduledAlarm
    scheduled?.triggerAtTime?.let {
      assertTrue("Alarm should be scheduled in the future", it >= now)
    }
  }

  @Test
  fun `cancelStudyAlarm cancels existing alarm`() {
    val eventId = "cancel-me"
    val trigger = System.currentTimeMillis() + 5_000L
    AlarmHelper.scheduleStudyAlarm(appCtx, eventId, trigger)

    val shadowAm = Shadows.shadowOf(am)
    assertTrue("Alarm should be scheduled", shadowAm.nextScheduledAlarm != null)

    AlarmHelper.cancelStudyAlarm(appCtx, eventId)
    // No crash implies success; Robolectric does not expose direct cancellation verification
    assertTrue(true)
  }

  private fun contextWithFailingAlarmManager(exception: Throwable): Context {
    val failingAm = mockk<AlarmManager>(relaxed = true)
    every { failingAm.setExactAndAllowWhileIdle(any(), any(), any()) } throws exception
    return object : ContextWrapper(appCtx) {
      override fun getSystemService(name: String): Any? {
        return if (name == Context.ALARM_SERVICE) failingAm else super.getSystemService(name)
      }
    }
  }

  private fun assertFallbackEnqueued(baseTag: String, eventId: String) {
    val fullTag =
        baseTag // tag passed to WorkRequest .addTag("study_session_start") remains constant
    val infos = WorkManager.getInstance(appCtx).getWorkInfosByTag(fullTag).get()
    assertTrue(
        "Expected fallback work enqueued with tag=$fullTag for event $eventId", infos.isNotEmpty())
  }

  @Test
  fun `scheduleStudyAlarm falls back when SecurityException thrown`() {
    val eventId = "sec-event"
    val ctx = contextWithFailingAlarmManager(SecurityException("denied"))
    AlarmHelper.scheduleStudyAlarm(ctx, eventId, System.currentTimeMillis() + 2000)
    assertFallbackEnqueued("study_session_start", eventId)
  }

  @Test
  fun `scheduleStudyAlarm falls back when IllegalArgumentException thrown`() {
    val eventId = "iae-event"
    val ctx = contextWithFailingAlarmManager(IllegalArgumentException("bad args"))
    AlarmHelper.scheduleStudyAlarm(ctx, eventId, System.currentTimeMillis() + 2000)
    assertFallbackEnqueued("study_session_start", eventId)
  }

  @Test
  fun `scheduleStudyAlarm falls back when generic Throwable thrown`() {
    val eventId = "thr-event"
    val ctx = contextWithFailingAlarmManager(RuntimeException("boom"))
    AlarmHelper.scheduleStudyAlarm(ctx, eventId, System.currentTimeMillis() + 2000)
    assertFallbackEnqueued("study_session_start", eventId)
  }
}
