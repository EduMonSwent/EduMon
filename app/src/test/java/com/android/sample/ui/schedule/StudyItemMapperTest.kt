package com.android.sample.ui.schedule

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.data.calendar.Priority as ModelPriority
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.Priority
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.data.schedule.SourceTag
import com.android.sample.feature.schedule.repository.schedule.StudyItemMapper
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StudyItemMapperTest {

  private val resources: Resources =
      ApplicationProvider.getApplicationContext<android.content.Context>().resources

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

    val ev = StudyItemMapper.toScheduleEvent(item, resources)

    assertEquals(item.id, ev.id)
    assertEquals("Study Linear Algebra", ev.title)
    assertEquals("Ch. 3", ev.description)
    assertEquals(item.date, ev.date)
    assertEquals(item.time, ev.time)
    assertEquals(90, ev.durationMinutes)
    assertEquals(EventKind.STUDY, ev.kind)
    assertFalse(ev.isCompleted)
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

    val item = StudyItemMapper.fromScheduleEvent(ev, resources)

    assertEquals(ev.id, item.id)
    assertEquals("Study OS", item.title)
    assertEquals(ev.date, item.date)
    assertEquals(ev.time, item.time)
    assertEquals(60, item.durationMinutes)
    assertTrue(item.isCompleted)
    assertEquals(ModelPriority.MEDIUM, item.priority)
    assertEquals(TaskType.WORK, item.type) // STUDY maps to WORK in mapper
  }

  @Test
  fun toScheduleEvent_guessWorkKind_projectByKeyword() {
    val item =
        StudyItem(
            title = "Project report",
            description = "final deadline",
            date = LocalDate.of(2025, 11, 10),
            type = TaskType.WORK,
            priority = ModelPriority.MEDIUM)

    val ev = StudyItemMapper.toScheduleEvent(item, resources)
    assertEquals(EventKind.SUBMISSION_PROJECT, ev.kind)
  }

  @Test
  fun toScheduleEvent_guessActivityKind_sportByKeyword() {
    val item =
        StudyItem(
            title = "Gym session",
            date = LocalDate.of(2025, 11, 9),
            priority = ModelPriority.MEDIUM,
            type = TaskType.PERSONAL)

    val ev = StudyItemMapper.toScheduleEvent(item, resources)
    assertEquals(EventKind.ACTIVITY_SPORT, ev.kind)
    assertNull(ev.priority) // PERSONAL has null priority in mapper
  }
}
