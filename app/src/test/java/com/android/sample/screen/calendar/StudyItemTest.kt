package com.android.sample.screen.calendar

import com.android.sample.feature.schedule.data.calendar.CalendarUiState
import com.android.sample.feature.schedule.data.calendar.Priority
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.*
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
    assertTrue(changed.isCompleted)
    assertEquals(60, changed.durationMinutes)
    assertEquals(d, changed.date)
    assertEquals(TaskType.STUDY, changed.type)
  }

  @Test
  fun default_values_are_set_and_uuid_is_generated() {
    val d = LocalDate.of(2025, 1, 2)
    val item =
        StudyItem(
            title = "Untimed",
            date = d,
            type = TaskType.WORK // leave description/time/duration at defaults
            )

    // id should be a non-empty random UUID
    assertTrue(item.id.isNotBlank())
    assertNull(item.description)
    assertNull(item.time)
    assertNull(item.durationMinutes)
    assertEquals(false, item.isCompleted)
    assertEquals(Priority.MEDIUM, item.priority)
    assertEquals(TaskType.WORK, item.type)
    assertEquals(d, item.date)
  }

  @Test
  fun uuid_is_unique_across_instances() {
    val d = LocalDate.of(2025, 1, 2)
    val items = (0 until 10).map { StudyItem(title = "t$it", date = d, type = TaskType.PERSONAL) }
    val distinctIds = items.map { it.id }.toSet()
    assertEquals(items.size, distinctIds.size)
  }

  @Test
  fun time_and_duration_are_preserved_through_copy() {
    val d = LocalDate.of(2025, 3, 4)
    val t = LocalTime.of(9, 30)
    val base =
        StudyItem(
            title = "Timed",
            date = d,
            time = t,
            durationMinutes = 45,
            type = TaskType.STUDY,
            priority = Priority.HIGH)
    val changed = base.copy(priority = Priority.LOW)

    assertEquals(t, changed.time)
    assertEquals(45, changed.durationMinutes)
    assertEquals(Priority.LOW, changed.priority)
    // ensure other fields unchanged
    assertEquals(d, changed.date)
    assertEquals("Timed", changed.title)
  }

  @Test
  fun equals_and_hashCode_match_on_all_fields_except_id_when_equal() {
    val d = LocalDate.of(2025, 5, 6)
    val t = LocalTime.of(14, 0)
    // same id -> equal
    val id = "fixed-id"
    val a =
        StudyItem(
            id = id,
            title = "Same",
            description = "D",
            date = d,
            time = t,
            durationMinutes = 30,
            isCompleted = true,
            priority = Priority.HIGH,
            type = TaskType.WORK)
    val b =
        StudyItem(
            id = id,
            title = "Same",
            description = "D",
            date = d,
            time = t,
            durationMinutes = 30,
            isCompleted = true,
            priority = Priority.HIGH,
            type = TaskType.WORK)

    assertEquals(a, b)
    assertEquals(a.hashCode(), b.hashCode())
  }

  @Test
  fun not_equal_when_ids_differ_even_if_other_fields_match() {
    val d = LocalDate.of(2025, 5, 6)
    val t = LocalTime.of(14, 0)
    val a =
        StudyItem(
            id = "A",
            title = "Same",
            description = "D",
            date = d,
            time = t,
            durationMinutes = 30,
            isCompleted = true,
            priority = Priority.HIGH,
            type = TaskType.WORK)
    val b =
        StudyItem(
            id = "B",
            title = "Same",
            description = "D",
            date = d,
            time = t,
            durationMinutes = 30,
            isCompleted = true,
            priority = Priority.HIGH,
            type = TaskType.WORK)

    assertNotEquals(a, b)
  }

  // ---------- CalendarUiState coverage ----------

  @Test
  fun calendarUiState_defaults_are_sensible() {
    val s = CalendarUiState()
    // Defaults
    assertTrue(s.tasksByDate.isEmpty())
    assertTrue(s.isMonthView)
    assertFalse(s.isShowingAddEditModal)
    assertNull(s.taskToEdit)
    // selectedDate/currentDisplayMonth default to "now" — assert they are consistent
    assertEquals(YearMonth.from(s.selectedDate), s.currentDisplayMonth)
  }

  @Test
  fun calendarUiState_custom_values_are_preserved() {
    val d1 = LocalDate.of(2025, 2, 10)
    val d2 = LocalDate.of(2025, 2, 11)
    val item1 = StudyItem(title = "A", date = d1, type = TaskType.STUDY)
    val item2 = StudyItem(title = "B", date = d2, type = TaskType.WORK)
    val map = mapOf(d1 to listOf(item1), d2 to listOf(item2))
    val ym = YearMonth.of(2025, 2)
    val s =
        CalendarUiState(
            tasksByDate = map,
            selectedDate = d1,
            currentDisplayMonth = ym,
            isMonthView = false,
            isShowingAddEditModal = true,
            taskToEdit = item2)

    assertEquals(map, s.tasksByDate)
    assertEquals(d1, s.selectedDate)
    assertEquals(ym, s.currentDisplayMonth)
    assertFalse(s.isMonthView)
    assertTrue(s.isShowingAddEditModal)
    assertEquals(item2, s.taskToEdit)
  }

  @Test
  fun calendarUiState_copy_toggles_modal_and_selection() {
    val d = LocalDate.of(2025, 7, 20)
    val i = StudyItem(title = "Edit me", date = d, type = TaskType.PERSONAL)
    val s = CalendarUiState(selectedDate = d, isShowingAddEditModal = false, taskToEdit = null)

    val s2 = s.copy(isShowingAddEditModal = true, taskToEdit = i)
    assertTrue(s2.isShowingAddEditModal)
    assertEquals(i, s2.taskToEdit)

    val nextMonth = s2.currentDisplayMonth.plusMonths(1)
    val s3 = s2.copy(currentDisplayMonth = nextMonth, isMonthView = true)
    assertEquals(nextMonth, s3.currentDisplayMonth)
    assertTrue(s3.isMonthView)
  }

  // ---------- Sorting / grouping sanity (hits more StudyItem fields) ----------

  @Test
  fun tasks_can_be_sorted_by_date_then_time_nulls_last() {
    val d = LocalDate.of(2025, 9, 1)
    val a = StudyItem(title = "A", date = d, time = LocalTime.of(8, 0), type = TaskType.STUDY)
    val b = StudyItem(title = "B", date = d, time = LocalTime.of(7, 30), type = TaskType.STUDY)
    val c = StudyItem(title = "C", date = d, time = null, type = TaskType.STUDY)
    val list =
        listOf(a, b, c)
            .sortedWith(compareBy<StudyItem> { it.date }.thenBy { it.time ?: LocalTime.MAX })
    assertEquals(listOf(b, a, c).map { it.title }, list.map { it.title })
  }
}
