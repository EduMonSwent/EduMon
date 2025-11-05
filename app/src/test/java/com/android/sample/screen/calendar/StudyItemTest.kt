package com.android.sample.screen.calendar

import com.android.sample.model.Priority
import com.android.sample.model.StudyItem
import com.android.sample.model.TaskType
import java.time.LocalDate
import junit.framework.TestCase.assertEquals
import org.junit.Test

class StudyItemTest {

  @Test
  fun copy_keepsUnchanged_and_overridesChanged() {
    val d = LocalDate.of(2025, 10, 14)
    val base =
        StudyItem(
            title = "Orig",
            description = "desc",
            date = d,
            durationMinutes = 60,
            isCompleted = false,
            priority = Priority.MEDIUM,
            type = TaskType.STUDY)
    val changed = base.copy(title = "New", isCompleted = true)
    assertEquals("New", changed.title)
    assertEquals(true, changed.isCompleted)
    assertEquals(60, changed.durationMinutes)
    assertEquals(d, changed.date)
    assertEquals(TaskType.STUDY, changed.type)
  }
}
