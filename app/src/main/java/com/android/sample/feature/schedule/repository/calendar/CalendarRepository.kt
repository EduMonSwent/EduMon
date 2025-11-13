package com.android.sample.feature.schedule.repository.calendar

import com.android.sample.feature.schedule.data.calendar.Priority
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface CalendarRepository {
  val tasksFlow: StateFlow<List<StudyItem>> // Expose tasks as a Flow for reactivity

  suspend fun getAllTasks(): List<StudyItem>

  suspend fun getTaskById(taskId: String): StudyItem?

  suspend fun saveTask(task: StudyItem)

  suspend fun deleteTask(taskId: String)
}

class CalendarRepositoryImpl : CalendarRepository {

  private val _tasks = MutableStateFlow<List<StudyItem>>(emptyList())
  override val tasksFlow: StateFlow<List<StudyItem>> = _tasks.asStateFlow()

  init {
    // Pre-populate with some dummy data
    _tasks.value =
        listOf(
            StudyItem(
                title = "Review Compose Basics",
                description = "Go through official docs on State and Side Effects.",
                date = LocalDate.now(),
                time = LocalTime.of(10, 0),
                isCompleted = false,
                priority = Priority.MEDIUM,
                type = TaskType.STUDY),
            StudyItem(
                title = "Plan Project Structure",
                description = "Define ViewModels and Repositories.",
                date = LocalDate.now().plusDays(1),
                time = LocalTime.of(14, 30),
                isCompleted = false,
                priority = Priority.HIGH,
                type = TaskType.WORK),
            StudyItem(
                title = "Read Clean Architecture",
                description = "Chapter 5: Boundaries.",
                date = LocalDate.now().minusDays(2),
                isCompleted = true,
                priority = Priority.LOW,
                type = TaskType.STUDY),
            StudyItem(
                title = "Implement Calendar UI",
                description = "Start with the custom grid.",
                date = LocalDate.now(),
                time = LocalTime.of(9, 0),
                isCompleted = false,
                priority = Priority.HIGH,
                type = TaskType.STUDY),
            StudyItem(
                title = "Yoga Session",
                date = LocalDate.now().plusWeeks(1),
                isCompleted = false,
                priority = Priority.LOW,
                type = TaskType.PERSONAL))
  }

  override suspend fun getAllTasks(): List<StudyItem> {
    return _tasks.value
  }

  override suspend fun getTaskById(taskId: String): StudyItem? {
    return _tasks.value.find { it.id == taskId }
  }

  override suspend fun saveTask(task: StudyItem) {
    val currentTasks = _tasks.value.toMutableList()
    val existingIndex = currentTasks.indexOfFirst { it.id == task.id }

    if (existingIndex >= 0) {
      currentTasks[existingIndex] = task // Update existing
    } else {
      currentTasks.add(task) // Add new
    }
    _tasks.value = currentTasks // Emit new list to flow
  }

  override suspend fun deleteTask(taskId: String) {
    _tasks.value = _tasks.value.filter { it.id != taskId } // Emit new list to flow
  }
}
