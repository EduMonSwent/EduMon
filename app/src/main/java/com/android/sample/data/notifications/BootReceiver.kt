package com.android.sample.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.feature.schedule.repository.calendar.CalendarRepository
import com.android.sample.repos_providors.AppRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-register alarms after device reboot. This receiver is invoked on BOOT_COMPLETED. It should
 * query the app's repository for upcoming study tasks and schedule alarms. Here we schedule via
 * AlarmHelper for each next upcoming task, a real implementation should avoid expensive operations
 * on the broadcast thread (use a JobIntentService or schedule a WorkManager job). For simplicity we
 * launch a coroutine here.
 */
class BootReceiver(
    private val repository: CalendarRepository = AppRepositories.calendarRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

    // Restart campus polling chain if it was enabled
    val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    val campusEnabled = prefs.getBoolean("campus_entry_enabled", false)
    if (campusEnabled) {
      CampusEntryPollWorker.startChain(context)
    }

    // Re-schedule next alarms in background
    scope.launch {
      try {
        // Use calendar repository to find upcoming tasks and schedule the next one(s)
        val tasks = repository.tasksFlow.value // best effort
        val zone = java.time.ZoneId.systemDefault()
        val now = java.time.Instant.now()

        val nextTask =
            tasks
                .asSequence()
                .filter { it.type == TaskType.STUDY }
                .filter { !it.isCompleted }
                .filter { it.time != null }
                .map { item ->
                  val dt = java.time.LocalDateTime.of(item.date, item.time)
                  Pair(item, dt.atZone(zone).toInstant())
                }
                .filter { (_, instant) -> instant.isAfter(now) }
                .sortedBy { it.second }
                .map { it.first }
                .firstOrNull()

        nextTask?.let {
          val taskDateTime = java.time.LocalDateTime.of(it.date, it.time!!)
          val trigger =
              taskDateTime.atZone(zone).toInstant().toEpochMilli() -
                  java.time.Duration.ofMinutes(15).toMillis()

          // Always try to schedule; if exact alarms are disallowed on API 31+, framework may throw
          // SecurityException. AlarmHelper is expected to internally fallback (e.g., WorkManager),
          // but we still catch SecurityException explicitly to satisfy lint and avoid crashy
          // receivers.
          try {
            AlarmHelper.scheduleStudyAlarm(context, it.id, trigger)
          } catch (se: SecurityException) {
            Log.w("BootReceiver", "Exact alarm not permitted; relying on AlarmHelper fallback", se)
          }
        }
      } catch (_: Exception) {
        // best-effort: ignore
      }
    }
  }
}
