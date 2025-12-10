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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    // Mock the Context resources required by KeywordMatcher
    every { context.resources } returns resources

    // Define keywords to match the logic in KeywordMatcher
    // These mocks ensure matcher.isExercise(), isLecture(), etc., return true/false correctly
    every { resources.getStringArray(R.array.ics_keywords_exercise) } returns
        arrayOf("exercise", "problem set")
    every { resources.getStringArray(R.array.ics_keywords_lab) } returns
        arrayOf("lab", "laboratory")
    every { resources.getStringArray(R.array.ics_keywords_project) } returns arrayOf("project")
    every { resources.getStringArray(R.array.ics_keywords_lecture) } returns
        arrayOf("lecture", "course", "theory")
    every { resources.getStringArray(R.array.ics_keywords_exam) } returns
        arrayOf("exam", "midterm", "final")

    importer = IcsImporter(plannerRepository, context)
  }

  @Test
  fun `maps exercise class type`() = runBlocking {
    // GIVEN an ICS stream with "Exercise" in the summary
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

    // Capture the list passed to saveClasses
    val slot = slot<List<Class>>()
    coEvery { plannerRepository.saveClasses(capture(slot)) } returns Result.success(Unit)

    // WHEN
    importer.importFromStream(ByteArrayInputStream(ics.toByteArray()))

    // THEN
    val saved = slot.captured
    assertEquals("Should import 1 class", 1, saved.size)
    // Verify type mapping logic: 'Exercise' keyword -> ClassType.EXERCISE
    assertEquals(ClassType.EXERCISE, saved[0].type)
  }

  @Test
  fun `lecture event is imported correctly`() = runBlocking {
    // GIVEN an ICS stream with typical lecture details
    // Note: New logic uses the description directly as instructor
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

    // WHEN
    importer.importFromStream(ByteArrayInputStream(ics.toByteArray()))

    // THEN
    val saved = slot.captured.first()
    assertEquals("Physics Lecture", saved.courseName)
    assertEquals("Room A", saved.location)
    assertEquals(ClassType.LECTURE, saved.type)
    // Verify instructor matches the description directly (per your new code)
    assertEquals("Prof. Einstein", saved.instructor)
  }

  @Test
  fun `blank instructor yields empty string`() = runBlocking {
    // GIVEN an ICS with no DESCRIPTION field
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

    // WHEN
    importer.importFromStream(ByteArrayInputStream(ics.toByteArray()))

    // THEN
    val saved = slot.captured.first()
    assertEquals("", saved.instructor)
  }
}
