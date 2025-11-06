package com.android.sample.schedule

import com.android.sample.model.Priority as ModelPriority
import com.android.sample.model.StudyItem
import com.android.sample.model.TaskType
import com.android.sample.model.schedule.EventKind
import com.android.sample.model.schedule.Priority
import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.SourceTag
import com.android.sample.model.schedule.StudyItemMapper
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class StudyItemMapperTest {

  @Test
  fun toScheduleEvent_mapsBasicFields_andPriority() {
    val item =
        StudyItem(
            title = "Study Linear Algebra",
            description = "Ch. 3",
            date = LocalDate.of(2025, 11, 7),
            time = LocalTime.of(14, 0),
            durationMinutes = 90,
            isCompleted = false,
            priority = ModelPriority.HIGH,
            type = TaskType.STUDY)

    val ev = StudyItemMapper.toScheduleEvent(item)

    assertEquals(item.id, ev.id)
    assertEquals("Study Linear Algebra", ev.title)
    assertEquals("Ch. 3", ev.description)
    assertEquals(item.date, ev.date)
    assertEquals(item.time, ev.time)
    assertEquals(90, ev.durationMinutes)
    assertEquals(EventKind.STUDY, ev.kind)
    assertEquals(false, ev.isCompleted)
    assertEquals(Priority.HIGH, ev.priority)
    assertEquals(SourceTag.Task, ev.sourceTag)
  }

  @Test
  fun fromScheduleEvent_roundTrips_forStudyKind() {
    val ev =
        ScheduleEvent(
            title = "Study OS",
            date = LocalDate.of(2025, 11, 8),
            time = LocalTime.of(9, 0),
            durationMinutes = 60,
            kind = EventKind.STUDY,
            isCompleted = true,
            priority = Priority.MEDIUM,
            sourceTag = SourceTag.Task)

    val item = StudyItemMapper.fromScheduleEvent(ev)

    assertEquals(ev.id, item.id)
    assertEquals("Study OS", item.title)
    assertEquals(ev.date, item.date)
    assertEquals(ev.time, item.time)
    assertEquals(60, item.durationMinutes)
    assertTrue(item.isCompleted)
    assertEquals(ModelPriority.MEDIUM, item.priority)
    // STUDY & SUBMISSION kinds map to WORK in your mapper
    assertEquals(TaskType.WORK, item.type)
  }

  @Test
  fun toScheduleEvent_guessWorkKind_projectByKeyword() {
    val item =
        StudyItem(
            title = "Project report",
            description = "final deadline",
            date = LocalDate.of(2025, 11, 10),
            type = TaskType.WORK)

    val ev = StudyItemMapper.toScheduleEvent(item)
    assertEquals(EventKind.SUBMISSION_PROJECT, ev.kind)
  }

  @Test
  fun toScheduleEvent_guessActivityKind_sportByKeyword() {
    val item =
        StudyItem(title = "Gym session", date = LocalDate.of(2025, 11, 9), type = TaskType.PERSONAL)

    val ev = StudyItemMapper.toScheduleEvent(item)
    assertEquals(EventKind.ACTIVITY_SPORT, ev.kind)
    assertNull(ev.priority) // per mapper for PERSONAL
  }
}
