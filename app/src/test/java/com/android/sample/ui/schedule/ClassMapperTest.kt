package com.android.sample.ui.schedule

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.android.sample.R
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClassMapperTest {

  private val resources: Resources =
      ApplicationProvider.getApplicationContext<android.content.Context>().resources

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

    val ev = ClassMapper.toScheduleEvent(c, resources)
    val at = resources.getString(R.string.keyword_at)
    val with = resources.getString(R.string.with)
    val expectedDesc =
        resources.getString(
            R.string.class_description_fmt, c.type, at, c.location, with, c.instructor)

    assertEquals(c.id, ev.id)
    assertEquals("Algorithms", ev.title)
    assertEquals(EventKind.CLASS_LECTURE, ev.kind)
    assertEquals(expectedDesc, ev.description)
    assertEquals(c.startTime, ev.time)
    assertEquals(120, ev.durationMinutes)
    assertEquals(SourceTag.Class, ev.sourceTag)
    assertEquals(LocalDate.now(), ev.date) // current implementation
  }

  @Test
  fun fromScheduleEvent_reconstructsPlannerClass_whenSourceIsClass() {
    val at = resources.getString(R.string.keyword_at)
    val with = resources.getString(R.string.with)

    val ev =
        ScheduleEvent(
            title = "CS101",
            date = LocalDate.now(),
            time = LocalTime.of(8, 30),
            durationMinutes = 90,
            kind = EventKind.CLASS_EXERCISE,
            description = "EXERCISE $at B2 $with Dr. Who",
            location = "B2",
            sourceTag = SourceTag.Class)

    val pc = ClassMapper.fromScheduleEvent(ev, resources)
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
    assertNull(ClassMapper.fromScheduleEvent(ev, resources))
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
    val pc = ClassMapper.fromScheduleEvent(ev, resources)
    requireNotNull(pc)
    assertEquals(resources.getString(R.string.keyword_professor_default), pc.instructor)
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
    val ev = ClassMapper.toScheduleEvent(c, resources)
    assertEquals(120, ev.durationMinutes)
  }
}
