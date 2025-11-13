// app/src/main/java/com/android/sample/ui/notifications/NotificationsViewModel.kt
package com.android.sample.ui.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.notifications.AlarmHelper
import com.android.sample.data.notifications.NotificationUtils
import com.android.sample.data.notifications.WorkManagerNotificationRepository
import com.android.sample.model.StudyItem
import com.android.sample.model.notifications.NotificationKind
import com.android.sample.model.notifications.NotificationRepository
import com.android.sample.repos_providors.AppRepositories
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repo: NotificationRepository = WorkManagerNotificationRepository()
) : ViewModel() {

  // --- Kickoff (configurable)
  private val _kickoffEnabled = MutableStateFlow(true)
  val kickoffEnabled: StateFlow<Boolean> = _kickoffEnabled.asStateFlow()

  // --- Task notifications (15 minutes before next task)
  // Separate from the Study kickoff feature. Enabled by default per request.
  private val _taskNotificationsEnabled = MutableStateFlow(true)
  val taskNotificationsEnabled: StateFlow<Boolean> = _taskNotificationsEnabled.asStateFlow()

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
    if (!on) {
      repo.cancel(ctx, NotificationKind.NO_WORK_TODAY)
    } else {
      // only manage kickoff schedule (weekly), do not start/stop task notifications here
    }
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

  // Task notifications (15 minutes before next upcoming Study task)
  fun setTaskNotificationsEnabled(ctx: Context, on: Boolean) {
    _taskNotificationsEnabled.value = on
    if (!on) {
      // stop observing and cancel any scheduled alarm
      scheduleObserverJob?.cancel()
      scheduleObserverJob = null
      scheduledTaskId?.let {
        try {
          AlarmHelper.cancelStudyAlarm(ctx, it)
        } catch (_: Exception) {}
      }
      scheduledTaskId = null
    } else {
      startObservingSchedule(ctx)
    }
  }

  private fun initWeek(h: Int, m: Int) = allDays.associateWith { h to m }

  // --- Reactive scheduling state
  private var scheduledTaskId: String? = null
  private var scheduleObserverJob: Job? = null

  /**
   * Start observing the planner/task flow and schedule a one-shot WorkManager reminder 15 minutes
   * before the next study task. Call this once (for example from the Notifications UI when the user
   * enables study-session reminders). The observation runs until ViewModel is cleared or until
   * `setKickoffEnabled(..., false)` is called.
   */
  @OptIn(FlowPreview::class)
  fun startObservingSchedule(ctx: Context) {
    // Avoid launching multiple collectors
    if (scheduleObserverJob?.isActive == true) return

    scheduleObserverJob =
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
          try {
            val planner = AppRepositories.calendarRepository
            planner.tasksFlow
                .debounce(1000) // wait briefly to avoid rapid rescheduling on bursts
                .collect { tasks ->
                  try {
                    // Determine next study task with a time in the future
                    val now = Instant.now()
                    val zone = ZoneId.systemDefault()

                    val nextTask =
                        tasks
                            .asSequence()
                            .filter { it.type == com.android.sample.model.TaskType.STUDY }
                            .filter { !it.isCompleted }
                            .filter { it.time != null }
                            .map { item ->
                              val dt = LocalDateTime.of(item.date, item.time)
                              Pair(item, dt.atZone(zone).toInstant())
                            }
                            .filter { (_, instant) -> instant.isAfter(now) }
                            .sortedBy { it.second }
                            .map { it.first }
                            .firstOrNull()

                    if (nextTask == null) {
                      // No upcoming study task: cancel any scheduled reminder
                      scheduledTaskId?.let { AlarmHelper.cancelStudyAlarm(ctx, it) }
                      scheduledTaskId = null
                      return@collect
                    }

                    // If already scheduled for the same task, do nothing
                    if (nextTask.id == scheduledTaskId) return@collect

                    // Schedule for this next task: 15 minutes before
                    val taskDateTime = LocalDateTime.of(nextTask.date, nextTask.time!!)
                    val taskInstant = taskDateTime.atZone(zone).toInstant()
                    val triggerAt = taskInstant.toEpochMilli() - Duration.ofMinutes(15).toMillis()

                    // cancel previous scheduling for safety
                    scheduledTaskId?.let { AlarmHelper.cancelStudyAlarm(ctx, it) }

                    // schedule new (defensive)
                    try {
                      AlarmHelper.scheduleStudyAlarm(ctx, nextTask.id, triggerAt)
                    } catch (e: Exception) {
                      Log.e("NotificationsVM", "Failed to schedule alarm for ${nextTask.id}", e)
                    }
                    scheduledTaskId = nextTask.id
                  } catch (e: Exception) {
                    Log.e("NotificationsVM", "Error while processing tasksFlow collect", e)
                  }
                }
          } catch (e: Throwable) {
            // Fail-safe: log and don't crash the VM / UI
            Log.e("NotificationsVM", "Failed to start observing schedule", e)
          }
        }
  }

  // Keep the old one-shot helper (non-reactive) for backward compatibility
  fun scheduleNextStudySessionNotification(ctx: Context) {
    // fallback one-shot scheduling via AlarmHelper
    val planner = AppRepositories.calendarRepository
    val tasks =
        try {
          planner.tasksFlow.value
        } catch (_: Exception) {
          emptyList<StudyItem>()
        }

    val now = Instant.now()
    val zone = ZoneId.systemDefault()

    val nextTask =
        tasks
            .asSequence()
            .filter { it.type == com.android.sample.model.TaskType.STUDY }
            .filter { !it.isCompleted }
            .filter { it.time != null }
            .map { item ->
              val dt = LocalDateTime.of(item.date, item.time)
              Pair(item, dt.atZone(zone).toInstant())
            }
            .filter { (_, instant) -> instant.isAfter(now) }
            .sortedBy { it.second }
            .map { it.first }
            .firstOrNull()

    if (nextTask == null) return

    // compute 15 minutes before task start
    val taskDateTime = LocalDateTime.of(nextTask.date, nextTask.time!!)
    val taskInstant = taskDateTime.atZone(zone).toInstant()
    val triggerAt = taskInstant.toEpochMilli() - Duration.ofMinutes(15).toMillis()

    // cancel previous
    scheduledTaskId?.let { AlarmHelper.cancelStudyAlarm(ctx, it) }

    AlarmHelper.scheduleStudyAlarm(ctx, nextTask.id, triggerAt)
    scheduledTaskId = nextTask.id
  }

  // Demo notification id
  companion object {
    private const val DEMO_NOTIFICATION_ID = 9001
  }

  /**
   * Sends an immediate demo notification that deep-links into the app's StudySessionScreen. It will
   * use the next scheduled study task id when available; otherwise a demo id. Caller should ensure
   * POST_NOTIFICATIONS permission is granted on Android 13+.
   */
  fun sendDeepLinkDemoNotification(ctx: Context) {
    try {
      val planner = AppRepositories.calendarRepository
      val tasks =
          try {
            planner.tasksFlow.value
          } catch (_: Exception) {
            emptyList<StudyItem>()
          }

      val now = Instant.now()
      val zone = ZoneId.systemDefault()

      val nextTask =
          tasks
              .asSequence()
              .filter { it.type == com.android.sample.model.TaskType.STUDY }
              .filter { !it.isCompleted }
              .filter { it.time != null }
              .map { item ->
                val dt = LocalDateTime.of(item.date, item.time)
                Pair(item, dt.atZone(zone).toInstant())
              }
              .filter { (_, instant) -> instant.isAfter(now) }
              .sortedBy { it.second }
              .map { it.first }
              .firstOrNull()

      val eventId = nextTask?.id ?: "demo"
      val deepLink = ctx.getString(com.android.sample.R.string.deep_link_format, eventId)

      // Build pending intent for deep link
      val intent =
          Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply { `package` = ctx.packageName }
      val pi =
          PendingIntent.getActivity(
              ctx,
              eventId.hashCode(),
              intent,
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

      // Ensure channel
      NotificationUtils.ensureChannel(ctx)

      // Permission check for POST_NOTIFICATIONS (Android 13+)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted =
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) return
      }

      val n =
          NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID)
              .setSmallIcon(com.android.sample.R.mipmap.ic_launcher)
              .setContentTitle(ctx.getString(com.android.sample.R.string.demo_notification_title))
              .setContentText(ctx.getString(com.android.sample.R.string.demo_notification_text))
              .setContentIntent(pi)
              .setAutoCancel(true)
              .build()

      NotificationManagerCompat.from(ctx).notify(DEMO_NOTIFICATION_ID, n)
    } catch (_: Exception) {
      // best-effort demo, swallow errors
    }
  }

  override fun onCleared() {
    super.onCleared()
    // Cancel the observer job when the VM is cleared
    scheduleObserverJob?.cancel()
    scheduleObserverJob = null
  }
}
