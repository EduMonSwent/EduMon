package com.android.sample.schedule

import com.android.sample.model.planner.Class as PlannerClass
import com.android.sample.model.planner.ClassType
import com.android.sample.model.schedule.ClassMapper
import com.android.sample.model.schedule.EventKind
import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.SourceTag
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
}
