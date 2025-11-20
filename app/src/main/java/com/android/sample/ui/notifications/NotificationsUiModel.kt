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
  val campusEntryEnabled: StateFlow<Boolean> // new toggle for campus entry notifications

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

  fun setCampusEntryEnabled(ctx: Context, on: Boolean) // enable/disable campus entry notifications

  fun startObservingSchedule(ctx: Context)

  // Background location permission helpers for campus entry feature
  fun needsBackgroundLocationPermission(ctx: Context): Boolean

  fun hasBackgroundLocationPermission(ctx: Context): Boolean

  fun requestBackgroundLocationIfNeeded(ctx: Context, launcher: (String) -> Unit)
}
