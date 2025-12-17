package com.android.sample.ui.schedule

import android.content.Context
import android.content.res.Resources
import com.android.sample.R
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.repository.schedule.IcsImporter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.ByteArrayInputStream
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IcsImporterTest {

  private lateinit var context: Context
  private lateinit var resources: Resources
  private lateinit var plannerRepository: PlannerRepository
  private lateinit var importer: IcsImporter

  @Before
  fun setup() {
    context = mockk()
    resources = mockk()
    plannerRepository = mockk(relaxed = true)

    every { context.resources } returns resources

    // Mock keywords
    every { resources.getStringArray(R.array.ics_keywords_exercise) } returns
        arrayOf("exercise", "tp")
    every { resources.getStringArray(R.array.ics_keywords_lab) } returns arrayOf("lab")
    every { resources.getStringArray(R.array.ics_keywords_project) } returns arrayOf("project")
    every { resources.getStringArray(R.array.ics_keywords_lecture) } returns
        arrayOf("lecture", "course")
    every { resources.getStringArray(R.array.ics_keywords_exam) } returns arrayOf("exam")

    importer = IcsImporter(plannerRepository, context)
  }

  @Test
  fun `maps exercise class type`() = runBlocking {
    val ics =
        """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Math Exercise
            DTSTART:20250101T100000
            DTEND:20250101T120000
            END:VEVENT
            END:VCALENDAR
        """
            .trimIndent()

    val slot = slot<List<Class>>()
    coEvery { plannerRepository.saveClasses(capture(slot)) } returns Result.success(Unit)

    importer.importFromStream(ByteArrayInputStream(ics.toByteArray()))

    val saved = slot.captured
    assertEquals(1, saved.size)
    assertEquals(ClassType.EXERCISE, saved[0].type)
  }

  @Test
  fun `lecture event is imported correctly`() = runBlocking {
    val ics =
        """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Physics Lecture
            DTSTART:20250102T140000
            DTEND:20250102T160000
            LOCATION:Room A
            DESCRIPTION:Prof. Einstein
            END:VEVENT
            END:VCALENDAR
        """
            .trimIndent()

    val slot = slot<List<Class>>()
    coEvery { plannerRepository.saveClasses(capture(slot)) } returns Result.success(Unit)

    importer.importFromStream(ByteArrayInputStream(ics.toByteArray()))

    val saved = slot.captured.first()
    assertEquals("Physics Lecture", saved.courseName)
    assertEquals("Room A", saved.location)
    assertEquals(ClassType.LECTURE, saved.type)
    assertEquals("Prof. Einstein", saved.instructor)
  }

  @Test
  fun `blank instructor yields empty string`() = runBlocking {
    val ics =
        """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Mystery Class
            DTSTART:20250103T100000
            END:VEVENT
            END:VCALENDAR
        """
            .trimIndent()

    val slot = slot<List<Class>>()
    coEvery { plannerRepository.saveClasses(capture(slot)) } returns Result.success(Unit)

    importer.importFromStream(ByteArrayInputStream(ics.toByteArray()))

    val saved = slot.captured.first()
    assertEquals("", saved.instructor)
  }

  @Test
  fun `exam event is skipped`() = runBlocking {
    val ics =
        """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Final Exam
            DTSTART:20250105T090000
            CATEGORIES:EXAM
            END:VEVENT
            END:VCALENDAR
        """
            .trimIndent()

    val slot = slot<List<Class>>()
    coEvery { plannerRepository.saveClasses(capture(slot)) } returns Result.success(Unit)

    importer.importFromStream(ByteArrayInputStream(ics.toByteArray()))

    // Should save an empty list
    val saved = slot.captured
    assertTrue("Exam event should be skipped", saved.isEmpty())
  }

  @Test
  fun `all-day event is skipped`() = runBlocking {
    val ics =
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        SUMMARY:All Day Event
        DTSTART;VALUE=DATE:20250101
        END:VEVENT
        END:VCALENDAR
      """
            .trimIndent()

    val slot = slot<List<Class>>()
    coEvery { plannerRepository.saveClasses(capture(slot)) } returns Result.success(Unit)

    importer.importFromStream(ByteArrayInputStream(ics.toByteArray()))

    assertTrue(slot.captured.isEmpty())
  }

  @Test
  fun `missing end time defaults to plus one hour`() = runBlocking {
    val ics =
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        SUMMARY:Short Class
        DTSTART:20250101T100000
        END:VEVENT
        END:VCALENDAR
      """
            .trimIndent()

    val slot = slot<List<Class>>()
    coEvery { plannerRepository.saveClasses(capture(slot)) } returns Result.success(Unit)

    importer.importFromStream(ByteArrayInputStream(ics.toByteArray()))

    val saved = slot.captured.first()
    assertEquals(LocalTime.of(10, 0), saved.startTime)
    assertEquals(LocalTime.of(11, 0), saved.endTime)
  }
}
