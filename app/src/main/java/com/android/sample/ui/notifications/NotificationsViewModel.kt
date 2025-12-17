package com.android.sample.ui.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.notifications.AlarmHelper
import com.android.sample.data.notifications.NotificationUtils
import com.android.sample.data.notifications.WorkManagerNotificationRepository
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.feature.schedule.repository.calendar.CalendarRepository
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

// Parts of this code were written with ChatGPT assistance

class NotificationsViewModel(
    private val repo: NotificationRepository = WorkManagerNotificationRepository(),
    private val calendarRepository: CalendarRepository = AppRepositories.calendarRepository,
    private val observeDispatcher: kotlinx.coroutines.CoroutineDispatcher =
        kotlinx.coroutines.Dispatchers.Default,
    private val debounceMillis: Long = 1000L
) : ViewModel(), NotificationsUiModel {

  // -------------------------------------------------------------------------
  // Helpers to remove duplicated code
  // -------------------------------------------------------------------------

  @VisibleForTesting
  internal fun safeTasksValue(repo: CalendarRepository): List<StudyItem> =
      try {
        repo.tasksFlow.value
      } catch (_: Exception) {
        emptyList()
      }

  private fun findNextUpcomingStudyTask(tasks: List<StudyItem>): StudyItem? {
    val now = Instant.now()
    val zone = ZoneId.systemDefault()

    return tasks
        .asSequence()
        .filter { it.type == TaskType.STUDY }
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
  }

  private fun computeTriggerAt(task: StudyItem): Long {
    val zone = ZoneId.systemDefault()
    val dt = LocalDateTime.of(task.date, task.time!!)
    val instant = dt.atZone(zone).toInstant()
    return instant.toEpochMilli() - Duration.ofMinutes(15).toMillis()
  }

  // -------------------------------------------------------------------------
  // UI Model State
  // -------------------------------------------------------------------------

  private val _kickoffEnabled = MutableStateFlow(true)
  override val kickoffEnabled: StateFlow<Boolean> = _kickoffEnabled.asStateFlow()

  private val _taskNotificationsEnabled = MutableStateFlow(true)
  override val taskNotificationsEnabled: StateFlow<Boolean> =
      _taskNotificationsEnabled.asStateFlow()

  private val allDays =
      setOf(
          Calendar.MONDAY,
          Calendar.TUESDAY,
          Calendar.WEDNESDAY,
          Calendar.THURSDAY,
          Calendar.FRIDAY,
          Calendar.SATURDAY,
          Calendar.SUNDAY)

  private val _kickoffDays = MutableStateFlow(emptySet<Int>())
  override val kickoffDays: StateFlow<Set<Int>> = _kickoffDays.asStateFlow()

  private val _kickoffTimes = MutableStateFlow(initWeek(9, 0))
  override val kickoffTimes: StateFlow<Map<Int, Pair<Int, Int>>> = _kickoffTimes.asStateFlow()

  private val _streakEnabled = MutableStateFlow(false)
  override val streakEnabled: StateFlow<Boolean> = _streakEnabled.asStateFlow()

  private val _campusEntryEnabled = MutableStateFlow(false)
  override val campusEntryEnabled: StateFlow<Boolean> = _campusEntryEnabled.asStateFlow()
  private val _friendStudyModeEnabled = MutableStateFlow(false)
  override val friendStudyModeEnabled: StateFlow<Boolean> = _friendStudyModeEnabled.asStateFlow()

  private val DEFAULT_STREAK_HOUR = 19

  override fun scheduleTestNotification(ctx: Context) = repo.scheduleOneMinuteFromNow(ctx)

  @Suppress("InlinedApi")
  override fun needsNotificationPermission(ctx: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val permission = Manifest.permission.POST_NOTIFICATIONS
    return ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED
  }

  @Suppress("InlinedApi")
  override fun requestOrSchedule(ctx: Context, permissionLauncher: (String) -> Unit) {
    val permission = Manifest.permission.POST_NOTIFICATIONS
    if (needsNotificationPermission(ctx)) {
      permissionLauncher(permission)
    } else {
      scheduleTestNotification(ctx)
    }
  }

  override fun setKickoffEnabled(ctx: Context, on: Boolean) {
    _kickoffEnabled.value = on
    if (!on) {
      repo.cancel(ctx, NotificationKind.NO_WORK_TODAY)
    }
  }

  override fun toggleKickoffDay(day: Int) {
    _kickoffDays.value =
        if (_kickoffDays.value.contains(day)) _kickoffDays.value - day else _kickoffDays.value + day
  }

  override fun updateKickoffTime(day: Int, hour: Int, minute: Int) {
    _kickoffTimes.value =
        _kickoffTimes.value.toMutableMap().apply {
          this[day] = hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
        }
  }

  override fun applyKickoffSchedule(ctx: Context) {
    if (!_kickoffEnabled.value) return
    val map = _kickoffTimes.value.filterKeys { _kickoffDays.value.contains(it) }
    repo.setWeeklySchedule(ctx, NotificationKind.NO_WORK_TODAY, true, map)
  }

  override fun setStreakEnabled(ctx: Context, on: Boolean) {
    _streakEnabled.value = on
    repo.setDailyEnabled(ctx, NotificationKind.KEEP_STREAK, on, DEFAULT_STREAK_HOUR)
  }

  override fun setCampusEntryEnabled(ctx: Context, on: Boolean) {
    _campusEntryEnabled.value = on
    // Persist user preference for CampusEntryPollWorker to check
    try {
      ctx.getSharedPreferences("notifications", Context.MODE_PRIVATE)
          .edit()
          .putBoolean("campus_entry_enabled", on)
          .apply()
    } catch (e: Exception) {
      Log.e("NotificationsVM", "Failed to save campus entry preference", e)
    }
    // Note: Worker chain runs continuously regardless of this setting.
    // This preference only controls whether notifications are sent.
  }

  override fun setFriendStudyModeEnabled(ctx: Context, on: Boolean) {
    _friendStudyModeEnabled.value = on
    // persist user preference
    try {
      ctx.getSharedPreferences("notifications", Context.MODE_PRIVATE)
          .edit()
          .putBoolean("friend_study_mode_enabled", on)
          .apply()
    } catch (e: Exception) {
      Log.e("NotificationsVM", "Failed to persist friend_study_mode_enabled preference", e)
    }
    if (on) {
      com.android.sample.data.notifications.FriendStudyModeWorker.startChain(ctx)
    } else {
      com.android.sample.data.notifications.FriendStudyModeWorker.cancel(ctx)
    }
  }

  @SuppressLint("ScheduleExactAlarm")
  @androidx.annotation.RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
  override fun setTaskNotificationsEnabled(ctx: Context, on: Boolean) {
    _taskNotificationsEnabled.value = on
    if (!on) {
      scheduleObserverJob?.cancel()
      scheduleObserverJob = null
      scheduledTaskId?.let {
        try {
          AlarmHelper.cancelStudyAlarm(ctx, it)
        } catch (_: Exception) {
          Log.e("NotificationsVM", "Failed to cancel alarm for $it")
        }
      }
      scheduledTaskId = null
    } else {
      startObservingSchedule(ctx)
    }
  }

  private fun initWeek(h: Int, m: Int) = allDays.associateWith { h to m }

  private var scheduledTaskId: String? = null
  private var scheduleObserverJob: Job? = null

  @SuppressLint("ScheduleExactAlarm")
  @OptIn(FlowPreview::class)
  @androidx.annotation.RequiresPermission(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
  override fun startObservingSchedule(ctx: Context) {
    if (scheduleObserverJob?.isActive == true) return

    scheduleObserverJob =
        viewModelScope.launch(observeDispatcher) {
          try {
            calendarRepository.tasksFlow.debounce(debounceMillis).collect { tasks ->
              try {
                val nextTask = findNextUpcomingStudyTask(tasks)

                if (nextTask == null) {
                  scheduledTaskId?.let { AlarmHelper.cancelStudyAlarm(ctx, it) }
                  scheduledTaskId = null
                  return@collect
                }

                if (nextTask.id == scheduledTaskId) return@collect

                val triggerAt = computeTriggerAt(nextTask)

                scheduledTaskId?.let { AlarmHelper.cancelStudyAlarm(ctx, it) }
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
            Log.e("NotificationsVM", "Failed to start observing schedule", e)
          }
        }
  }

  @SuppressLint("ScheduleExactAlarm")
  @androidx.annotation.RequiresPermission(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
  fun scheduleNextStudySessionNotification(ctx: Context) {
    val tasks = safeTasksValue(calendarRepository)
    val nextTask = findNextUpcomingStudyTask(tasks) ?: return

    val triggerAt = computeTriggerAt(nextTask)

    scheduledTaskId?.let { AlarmHelper.cancelStudyAlarm(ctx, it) }
    AlarmHelper.scheduleStudyAlarm(ctx, nextTask.id, triggerAt)
    scheduledTaskId = nextTask.id
  }

  companion object {
    private const val DEMO_NOTIFICATION_ID = 9001
  }

  override fun sendDeepLinkDemoNotification(ctx: Context) {
    try {
      val tasks = safeTasksValue(calendarRepository)
      val nextTask = findNextUpcomingStudyTask(tasks)

      val eventId = nextTask?.id ?: "demo"
      val deepLink = ctx.getString(com.android.sample.R.string.deep_link_format, eventId)

      val intent =
          Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply { `package` = ctx.packageName }

      val pi =
          PendingIntent.getActivity(
              ctx,
              eventId.hashCode(),
              intent,
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

      NotificationUtils.ensureChannel(ctx)

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
              .setPriority(NotificationCompat.PRIORITY_DEFAULT)
              .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
              .build()

      NotificationManagerCompat.from(ctx).notify(DEMO_NOTIFICATION_ID, n)
    } catch (_: Exception) {
      Log.e("NotificationsVM", "Failed to send demo notification")
    }
  }

  override fun onCleared() {
    super.onCleared()
    scheduleObserverJob?.cancel()
    scheduleObserverJob = null
    // Do not cancel campus polling here; user preference drives it.
  }

  @VisibleForTesting internal fun currentScheduledTaskId(): String? = scheduledTaskId
}
