package com.android.sample.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.android.sample.ui.notifications.NotificationsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NotificationsViewModelCampusTest {

  private lateinit var viewModel: NotificationsViewModel
  private lateinit var context: Context
  private lateinit var workManager: WorkManager

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    WorkManagerTestInitHelper.initializeTestWorkManager(context)
    workManager = WorkManager.getInstance(context)
    viewModel = NotificationsViewModel()
  }

  @Test
  fun `setCampusEntryEnabled true persists preference`() {
    // When
    viewModel.setCampusEntryEnabled(context, true)

    // Then
    val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    assertTrue(prefs.getBoolean("campus_entry_enabled", false))
  }

  @Test
  fun `setCampusEntryEnabled false persists preference`() {
    // Given: initially enabled
    val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("campus_entry_enabled", true).commit()

    // When
    viewModel.setCampusEntryEnabled(context, false)

    // Then
    assertFalse(prefs.getBoolean("campus_entry_enabled", true))
  }

  @Test
  fun `setCampusEntryEnabled true starts worker chain`() = runTest {
    // When
    viewModel.setCampusEntryEnabled(context, true)

    // Then: verify work is enqueued
    val workInfos = workManager.getWorkInfosByTag("campus_entry_poll").get()
    assertTrue("Worker should be enqueued", workInfos.isNotEmpty())
  }

  @Test
  fun `setCampusEntryEnabled false cancels worker`() = runTest {
    // Given: worker running
    viewModel.setCampusEntryEnabled(context, true)

    // When
    viewModel.setCampusEntryEnabled(context, false)

    // Then: verify work is cancelled
    val workInfos = workManager.getWorkInfosByTag("campus_entry_poll").get()
    val runningWork =
        workInfos.filter {
          it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
        }
    assertTrue("No worker should be running", runningWork.isEmpty())
  }

  @Test
  fun `campusEntryEnabled flow emits correct initial state`() {
    // Then
    assertEquals(false, viewModel.campusEntryEnabled.value)
  }

  @Test
  fun `campusEntryEnabled flow emits updated state after toggle`() {
    // When
    viewModel.setCampusEntryEnabled(context, true)

    // Then
    assertEquals(true, viewModel.campusEntryEnabled.value)
  }

  @Test
  fun `setCampusEntryEnabled handles missing preferences gracefully`() {
    // When: set preference even if SharedPreferences theoretically fails
    viewModel.setCampusEntryEnabled(context, true)

    // Then: should not throw
    assertEquals(true, viewModel.campusEntryEnabled.value)
  }
}
