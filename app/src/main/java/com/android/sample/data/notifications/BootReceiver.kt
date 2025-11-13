package com.android.sample.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

    // Re-schedule next alarms in background
    CoroutineScope(Dispatchers.Default).launch {
      try {
        // Use calendar repository to find upcoming tasks and schedule the next one(s)
        val planner = com.android.sample.repos_providors.AppRepositories.calendarRepository
        val tasks = planner.tasksFlow.value // best effort
        val zone = java.time.ZoneId.systemDefault()
        val now = java.time.Instant.now()

        val nextTask =
            tasks
                .asSequence()
                .filter { it.type == com.android.sample.model.TaskType.STUDY }
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
          AlarmHelper.scheduleStudyAlarm(context, it.id, trigger)
        }
      } catch (_: Exception) {
        // best-effort: ignore
      }
    }
  }
}
