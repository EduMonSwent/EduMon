package com.android.sample.ui.schedule

import com.android.sample.model.StudyItem
import com.android.sample.model.calendar.PlannerRepository
import java.time.LocalDate

class WeeklyPlanAdjuster(private val taskRepo: PlannerRepository) {
  suspend fun rebalance(today: LocalDate = LocalDate.now()) {
    val tasks = taskRepo.getAllTasks()

    // Move missed tasks to same weekday next week
    tasks
        .filter { it.date.isBefore(today) && !it.isCompleted }
        .forEach { missed -> taskRepo.saveTask(missed.copy(date = missed.date.plusWeeks(1))) }

    // If something was completed early, pull one from next week into this week
    val completedEarly = tasks.any { it.date.isAfter(today) && it.isCompleted }
    if (completedEarly) {
      val nextWeekStart = today.plusWeeks(1).with(java.time.DayOfWeek.MONDAY)
      val nextWeekEnd = nextWeekStart.plusDays(6)
      val candidate =
          tasks
              .filter { it.date >= nextWeekStart && it.date <= nextWeekEnd && !it.isCompleted }
              .sortedWith(compareByDescending<StudyItem> { it.priority }.thenBy { it.date })
              .firstOrNull()
      if (candidate != null) {
        taskRepo.saveTask(candidate.copy(date = today))
      }
    }
  }
}
