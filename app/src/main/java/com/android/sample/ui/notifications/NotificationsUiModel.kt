package com.android.sample.ui.notifications

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface NotificationsUiModel {
  // State exposed to the UI
  val kickoffEnabled: StateFlow<Boolean>
  val kickoffDays: StateFlow<Set<Int>>
  val kickoffTimes: StateFlow<Map<Int, Pair<Int, Int>>>
  val taskNotificationsEnabled: StateFlow<Boolean>
  val streakEnabled: StateFlow<Boolean>

  // UI actions
  fun setKickoffEnabled(ctx: Context, on: Boolean)

  fun toggleKickoffDay(day: Int)

  fun updateKickoffTime(day: Int, hour: Int, minute: Int)

  fun applyKickoffSchedule(ctx: Context)

  fun setStreakEnabled(ctx: Context, on: Boolean)

  fun scheduleTestNotification(ctx: Context)

  fun needsNotificationPermission(ctx: Context): Boolean

  fun requestOrSchedule(ctx: Context, permissionLauncher: (String) -> Unit)

  fun sendDeepLinkDemoNotification(ctx: Context)

  fun setTaskNotificationsEnabled(ctx: Context, on: Boolean)

  fun startObservingSchedule(ctx: Context)
}
