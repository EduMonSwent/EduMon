package com.android.sample.data.notifications

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest

interface WorkScheduler {
  fun enqueueUniqueWork(name: String, policy: ExistingWorkPolicy, request: OneTimeWorkRequest)

  fun enqueueUniquePeriodicWork(
      name: String,
      policy: ExistingPeriodicWorkPolicy,
      request: PeriodicWorkRequest
  )

  fun cancelUniqueWork(name: String)
}
