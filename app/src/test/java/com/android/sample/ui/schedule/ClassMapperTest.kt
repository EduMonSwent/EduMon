package com.android.sample.ui.schedule

import com.android.sample.model.Priority as ModelPriority
import com.android.sample.model.StudyItem
import com.android.sample.model.TaskType
import com.android.sample.model.planner.Class as PlannerClass
import com.android.sample.model.planner.ClassType
import com.android.sample.model.schedule.ClassMapper
import com.android.sample.model.schedule.EventKind
import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.SourceTag
import com.android.sample.model.schedule.StudyItemMapper
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class ClassMapperTest {

  @Test
  fun toScheduleEvent_mapsClassFields() {
    val c =
        PlannerClass(
            courseName = "Algorithms",
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(12, 0),
            type = ClassType.LECTURE,
            location = "A1",
            instructor = "Prof. Smith")

    val ev = ClassMapper.toScheduleEvent(c)
    assertEquals(c.id, ev.id)
    assertEquals("Algorithms", ev.title)
    assertEquals(EventKind.CLASS_LECTURE, ev.kind)
    assertEquals("LECTURE at A1 with Prof. Smith", ev.description)
    assertEquals(c.startTime, ev.time)
    assertEquals(120, ev.durationMinutes)
    assertEquals(SourceTag.Class, ev.sourceTag)
    assertEquals(LocalDate.now(), ev.date) // by current implementation
  }

  @Test
  fun fromScheduleEvent_reconstructsPlannerClass_whenSourceIsClass() {
    val ev =
        ScheduleEvent(
            title = "CS101",
            date = LocalDate.now(),
            time = LocalTime.of(8, 30),
            durationMinutes = 90,
            kind = EventKind.CLASS_EXERCISE,
            description = "EXERCISE at B2 with Dr. Who",
            location = "B2",
            sourceTag = SourceTag.Class)

    val pc = ClassMapper.fromScheduleEvent(ev)
    requireNotNull(pc)
    assertEquals(ev.id, pc.id)
    assertEquals("CS101", pc.courseName)
    assertEquals(LocalTime.of(8, 30), pc.startTime)
    assertEquals(LocalTime.of(10, 0), pc.endTime)
    assertEquals(ClassType.EXERCISE, pc.type)
    assertEquals("B2", pc.location)
    assertEquals("Dr. Who", pc.instructor)
  }

  @Test
  fun fromScheduleEvent_returnsNull_forNonClassSource() {
    val ev =
        ScheduleEvent(
            title = "Study",
            date = LocalDate.now(),
            kind = EventKind.STUDY,
            sourceTag = SourceTag.Task)
    assertNull(ClassMapper.fromScheduleEvent(ev))
  }

  @Test
  fun guessWorkKind_exam_only_when_exam_keyword_present() {
    val item =
        StudyItem(
            title = "Final presentation",
            description = "final deadline",
            date = LocalDate.now(),
            type = TaskType.WORK)
    // “deadline” should beat “final” → submission (not exam)
    val ev = StudyItemMapper.toScheduleEvent(item)
    assertEquals(EventKind.SUBMISSION_PROJECT, ev.kind)

    val exam =
        StudyItem(
            title = "Final Exam - OS",
            description = "exam at 10",
            date = LocalDate.now(),
            type = TaskType.WORK)
    assertEquals(EventKind.EXAM_FINAL, StudyItemMapper.toScheduleEvent(exam).kind)
  }

  @Test
  fun class_fromScheduleEvent_defaults_instructor_when_missing_in_description() {
    val now = LocalDate.now()
    val ev =
        ScheduleEvent(
            title = "CS101",
            date = now,
            time = LocalTime.of(10, 0),
            durationMinutes = 60,
            kind = EventKind.CLASS_LAB,
            description = null,
            location = "L1",
            sourceTag = SourceTag.Class)
    val pc = ClassMapper.fromScheduleEvent(ev)
    requireNotNull(pc)
    assertEquals("Professor", pc.instructor) // default path
  }

  @Test
  fun mapEventKindToStudyItem_personal_defaults_priority_medium() {
    val ev =
        ScheduleEvent(
            title = "Club",
            date = LocalDate.now(),
            kind = EventKind.ACTIVITY_ASSOCIATION,
            sourceTag = SourceTag.Task)
    val back = StudyItemMapper.fromScheduleEvent(ev)
    assertEquals(TaskType.PERSONAL, back.type)
    assertEquals(ModelPriority.MEDIUM, back.priority)
  }

  @Test
  fun class_toScheduleEvent_duration_null_safe() {
    val c =
        PlannerClass(
            courseName = "Algo",
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(12, 0),
            type = ClassType.LECTURE,
            location = "R1",
            instructor = "Prof")
    val ev = ClassMapper.toScheduleEvent(c)
    assertEquals(120, ev.durationMinutes)
  }
}
