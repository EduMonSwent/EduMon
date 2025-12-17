package com.android.sample.data.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkManagerSchedulerTest {

  private lateinit var context: Context
  private lateinit var scheduler: WorkManagerScheduler

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    WorkManagerTestInitHelper.initializeTestWorkManager(context)
    scheduler = WorkManagerScheduler(context)
  }

  @Test
  fun enqueueUniqueWork_schedulesOneTimeWork() {
    val request = OneTimeWorkRequest.from(StudyKickoffWorker::class.java)

    scheduler.enqueueUniqueWork(
        name = "test-one-time-work", policy = ExistingWorkPolicy.REPLACE, request = request)

    val workInfo =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork("test-one-time-work").get()

    assert(workInfo.isNotEmpty())
  }

  @Test
  fun enqueueUniquePeriodicWork_schedulesPeriodicWork() {
    val request =
        PeriodicWorkRequest.Builder(StudyKickoffWorker::class.java, 15, TimeUnit.MINUTES).build()

    scheduler.enqueueUniquePeriodicWork(
        name = "test-periodic-work", policy = ExistingPeriodicWorkPolicy.KEEP, request = request)

    val workInfo =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork("test-periodic-work").get()

    assert(workInfo.isNotEmpty())
  }

  @Test
  fun cancelUniqueWork_cancelsScheduledWork() {
    val request = OneTimeWorkRequest.from(StudyKickoffWorker::class.java)

    scheduler.enqueueUniqueWork(
        name = "test-cancel-work", policy = ExistingWorkPolicy.REPLACE, request = request)

    scheduler.cancelUniqueWork("test-cancel-work")

    val workInfo =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork("test-cancel-work").get()

    // After cancellation, work should be cancelled
    assert(workInfo.isEmpty() || workInfo.all { it.state.isFinished })
  }
}
