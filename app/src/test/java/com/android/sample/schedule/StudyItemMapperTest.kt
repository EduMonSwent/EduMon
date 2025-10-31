package com.android.sample.schedule

import com.android.sample.model.Priority as TaskPriority
import com.android.sample.model.StudyItem
import com.android.sample.model.TaskType
import com.android.sample.model.schedule.EventKind
import com.android.sample.model.schedule.Priority
import com.android.sample.model.schedule.SourceTag
import com.android.sample.model.schedule.StudyItemMapper
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyItemMapperTest {

  private fun study(
      id: String = "id1",
      title: String = "Project milestone",
      desc: String? = null,
      date: LocalDate = LocalDate.of(2025, 3, 6),
      time: LocalTime? = LocalTime.of(10, 0),
      completed: Boolean = false,
      prio: TaskPriority = TaskPriority.HIGH,
      type: TaskType = TaskType.WORK,
      duration: Int? = 60
  ) =
      StudyItem(
          id = id,
          title = title,
          description = desc,
          date = date,
          time = time,
          durationMinutes = duration,
          isCompleted = completed,
          priority = prio,
          type = type)

  @Test
  fun toScheduleEvent_and_back_preserveCoreFields() {
    val s = study()
    val ev = StudyItemMapper.toScheduleEvent(s)
    assertEquals(s.id, ev.id)
    assertEquals(s.title, ev.title)
    assertEquals(s.description, ev.description)
    assertEquals(s.date, ev.date)
    assertEquals(s.time, ev.time)
    assertEquals(s.durationMinutes, ev.durationMinutes)
    assertEquals(SourceTag.Task, ev.sourceTag)

    val back = StudyItemMapper.fromScheduleEvent(ev)
    assertEquals(s.id, back.id)
    assertEquals(s.title, back.title)
    assertEquals(s.description, back.description)
    assertEquals(s.date, back.date)
    assertEquals(s.time, back.time)
    assertEquals(s.durationMinutes, back.durationMinutes)
    assertEquals(s.isCompleted, back.isCompleted)
  }

  @Test
  fun guessWorkKind_usesKeywords() {
    val mid = study(title = "Algo Midterm")
    val fin = study(title = "Final exam")
    val proj = study(title = "Mobile Project")
    val milestone = study(title = "Project Milestone 1")
    val weekly = study(title = "Weekly report")
    val due = study(title = "Something due tomorrow")

    assertEquals(EventKind.EXAM_MIDTERM, StudyItemMapper.toScheduleEvent(mid).kind)
    assertEquals(EventKind.EXAM_FINAL, StudyItemMapper.toScheduleEvent(fin).kind)
    assertEquals(EventKind.PROJECT, StudyItemMapper.toScheduleEvent(proj).kind)
    assertEquals(EventKind.SUBMISSION_MILESTONE, StudyItemMapper.toScheduleEvent(milestone).kind)
    assertEquals(EventKind.SUBMISSION_WEEKLY, StudyItemMapper.toScheduleEvent(weekly).kind)
    assertEquals(EventKind.SUBMISSION_PROJECT, StudyItemMapper.toScheduleEvent(due).kind)
  }

  @Test
  fun guessActivityKind_sportVsAssociation() {
    val sport = study(type = TaskType.PERSONAL, title = "Gym session")
    val assoc = study(type = TaskType.PERSONAL, title = "Association meetup")
    val unk = study(type = TaskType.PERSONAL, title = "Volunteering")

    assertEquals(EventKind.ACTIVITY_SPORT, StudyItemMapper.toScheduleEvent(sport).kind)
    assertEquals(EventKind.ACTIVITY_ASSOCIATION, StudyItemMapper.toScheduleEvent(assoc).kind)
    assertEquals(EventKind.ACTIVITY_ASSOCIATION, StudyItemMapper.toScheduleEvent(unk).kind)
  }

  @Test
  fun priorityConversions_roundTrip() {
    val low = study(prio = TaskPriority.LOW)
    val med = study(prio = TaskPriority.MEDIUM)
    val high = study(prio = TaskPriority.HIGH)

    assertEquals(Priority.LOW, StudyItemMapper.toScheduleEvent(low).priority)
    assertEquals(Priority.MEDIUM, StudyItemMapper.toScheduleEvent(med).priority)
    assertEquals(Priority.HIGH, StudyItemMapper.toScheduleEvent(high).priority)
  }
}
