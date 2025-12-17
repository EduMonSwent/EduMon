package com.android.sample.ui.schedule

import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.usecase.DailyClassObjectiveGenerator
import com.android.sample.feature.weeks.model.ObjectiveType
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyClassObjectiveGeneratorTest {

  private val generator = DailyClassObjectiveGenerator()

  private fun baseClass(name: String, type: ClassType): Class =
      Class(
          courseName = name,
          startTime = LocalTime.of(8, 0),
          endTime = LocalTime.of(10, 0),
          type = type)

  @Test
  fun `generate creates lecture objective correctly`() {
    val clazz = baseClass("CompSec", ClassType.LECTURE)

    val result =
        generator.generate(todayClasses = listOf(clazz), currentWeek = 12, day = DayOfWeek.MONDAY)

    val obj = result.single()

    assertEquals("Review CompSec lecture", obj.title)
    assertEquals("CompSec", obj.course)
    assertEquals(45, obj.estimateMinutes)
    assertEquals(DayOfWeek.MONDAY, obj.day)
    assertEquals(ObjectiveType.COURSE_OR_EXERCISES, obj.type)
    assertTrue(obj.isAuto)
    assertFalse(obj.completed)
    assertEquals("AUTO:CompSec:LECTURE:12", obj.sourceId)
  }

  @Test
  fun `generate creates exercise objective correctly`() {
    val clazz = baseClass("Math", ClassType.EXERCISE)

    val result =
        generator.generate(todayClasses = listOf(clazz), currentWeek = 5, day = DayOfWeek.TUESDAY)

    val obj = result.single()

    assertEquals("Do Math exercise set 5", obj.title)
    assertEquals(60, obj.estimateMinutes)
    assertEquals("AUTO:Math:EXERCISE:5", obj.sourceId)
  }

  @Test
  fun `generate creates lab objective correctly`() {
    val clazz = baseClass("Networks", ClassType.LAB)

    val result =
        generator.generate(todayClasses = listOf(clazz), currentWeek = 3, day = DayOfWeek.WEDNESDAY)

    val obj = result.single()

    assertEquals("Prepare Networks lab (week 3)", obj.title)
    assertEquals(90, obj.estimateMinutes)
    assertEquals("AUTO:Networks:LAB:3", obj.sourceId)
  }

  @Test
  fun `generate creates project objective correctly`() {
    val clazz = baseClass("AI", ClassType.PROJECT)

    val result =
        generator.generate(todayClasses = listOf(clazz), currentWeek = 7, day = DayOfWeek.THURSDAY)

    val obj = result.single()

    assertEquals("Work on AI project", obj.title)
    assertEquals(60, obj.estimateMinutes)
    assertEquals("AUTO:AI:PROJECT:7", obj.sourceId)
  }

  @Test
  fun `generate supports multiple classes and preserves order`() {
    val classes = listOf(baseClass("OS", ClassType.LECTURE), baseClass("OS", ClassType.EXERCISE))

    val result =
        generator.generate(todayClasses = classes, currentWeek = 10, day = DayOfWeek.FRIDAY)

    assertEquals(2, result.size)
    assertEquals("Review OS lecture", result[0].title)
    assertEquals("Do OS exercise set 10", result[1].title)
  }

  @Test
  fun `generate uses default day when not provided`() {
    val clazz = baseClass("DB", ClassType.LECTURE)

    val result = generator.generate(todayClasses = listOf(clazz), currentWeek = 1)

    assertEquals(java.time.LocalDate.now().dayOfWeek, result.single().day)
  }
}
