// app/src/main/java/com/android/sample/ui/notifications/NotificationsViewModel.kt
package com.android.sample.ui.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.android.sample.data.notifications.WorkManagerNotificationRepository
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationsViewModel(
    private val repo: NotificationRepository = WorkManagerNotificationRepository()
) : ViewModel() {

  // --- Kickoff (configurable)
  private val _kickoffEnabled = MutableStateFlow(false)
  val kickoffEnabled: StateFlow<Boolean> = _kickoffEnabled.asStateFlow()

  private val allDays =
      setOf(
          Calendar.MONDAY,
          Calendar.TUESDAY,
          Calendar.WEDNESDAY,
          Calendar.THURSDAY,
          Calendar.FRIDAY,
          Calendar.SATURDAY,
          Calendar.SUNDAY)
  private val _kickoffDays = MutableStateFlow(emptySet<Int>()) // start empty
  val kickoffDays: StateFlow<Set<Int>> = _kickoffDays.asStateFlow()

  private val _kickoffTimes = MutableStateFlow(initWeek(9, 0))
  val kickoffTimes: StateFlow<Map<Int, Pair<Int, Int>>> = _kickoffTimes.asStateFlow()

  // --- Streak (toggle only)
  private val _streakEnabled = MutableStateFlow(false)
  val streakEnabled: StateFlow<Boolean> = _streakEnabled.asStateFlow()

  private val DEFAULT_STREAK_HOUR = 19 // daily at 19:00

  fun scheduleTestNotification(ctx: Context) = repo.scheduleOneMinuteFromNow(ctx)

  /**
   * Indicates wether the permission is needed or not.
   *
   * @param ctx context
   */
  @Suppress("InlinedApi")
  fun needsNotificationPermission(ctx: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val permission = Manifest.permission.POST_NOTIFICATIONS
    return ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED
  }

  /**
   * Checks the permission and, if needed, requests it.
   *
   * @param ctx context
   * @param permissionLauncher launcher for the permission request
   */
  @Suppress("InlinedApi")
  fun requestOrSchedule(ctx: Context, permissionLauncher: (String) -> Unit) {
    val permission = Manifest.permission.POST_NOTIFICATIONS
    if (needsNotificationPermission(ctx)) {
      permissionLauncher(permission)
    } else {
      scheduleTestNotification(ctx)
    }
  }

  // Kickoff
  fun setKickoffEnabled(ctx: Context, on: Boolean) {
    _kickoffEnabled.value = on
    if (!on) repo.cancel(ctx, NotificationKind.NO_WORK_TODAY)
  }

  fun toggleKickoffDay(day: Int) {
    _kickoffDays.value =
        if (_kickoffDays.value.contains(day)) _kickoffDays.value - day else _kickoffDays.value + day
  }

  fun updateKickoffTime(day: Int, hour: Int, minute: Int) {
    _kickoffTimes.value =
        _kickoffTimes.value.toMutableMap().apply {
          this[day] = hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
        }
  }

  fun applyKickoffSchedule(ctx: Context) {
    if (!_kickoffEnabled.value) return
    val map = _kickoffTimes.value.filterKeys { _kickoffDays.value.contains(it) }
    repo.setWeeklySchedule(ctx, NotificationKind.NO_WORK_TODAY, true, map)
  }

  // Streak
  fun setStreakEnabled(ctx: Context, on: Boolean) {
    _streakEnabled.value = on
    repo.setDailyEnabled(ctx, NotificationKind.KEEP_STREAK, on, DEFAULT_STREAK_HOUR)
  }

  private fun initWeek(h: Int, m: Int) = allDays.associateWith { h to m }
}
