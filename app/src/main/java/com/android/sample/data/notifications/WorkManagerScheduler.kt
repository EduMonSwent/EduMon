package com.android.sample.data.notifications

import android.content.Context
import androidx.work.*

class WorkManagerScheduler(private val context: Context) : WorkScheduler {
  private val wm
    get() = WorkManager.getInstance(context)

  override fun enqueueUniqueWork(
      name: String,
      policy: ExistingWorkPolicy,
      request: OneTimeWorkRequest
  ) {
    wm.enqueueUniqueWork(name, policy, request)
  }

  override fun enqueueUniquePeriodicWork(
      name: String,
      policy: ExistingPeriodicWorkPolicy,
      request: PeriodicWorkRequest
  ) {
    wm.enqueueUniquePeriodicWork(name, policy, request)
  }

  override fun cancelUniqueWork(name: String) {
    wm.cancelUniqueWork(name)
  }
}
