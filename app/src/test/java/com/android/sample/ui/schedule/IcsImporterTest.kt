package com.android.sample.ui.schedule

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.repository.schedule.IcsImporter
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class IcsImporterTest {

  private lateinit var context: Context
  private lateinit var planner: PlannerRepository
  private lateinit var schedule: ScheduleRepository
  private lateinit var importer: IcsImporter

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    planner = mockk(relaxed = true)
    schedule = mockk(relaxed = true)
    importer = IcsImporter(schedule, planner, context)
  }

  private fun stream(text: String) = text.trimIndent().byteInputStream()

  @Test
  fun `exam events are skipped`() = runTest {
    val ics =
        """
            BEGIN:VEVENT
            SUMMARY:Exam
            DTSTART:20241201T090000
            CATEGORIES:Horaires examinés
            END:VEVENT
        """

    importer.importFromStream(stream(ics))

    coVerify(exactly = 0) { planner.saveClass(any()) }
  }

  @Test
  fun `lecture event is imported correctly`() = runTest {
    val ics =
        """
            BEGIN:VEVENT
            SUMMARY:Cours d'info
            DTSTART:20241201T101500
            DTEND:20241201T121500
            DESCRIPTION:Prof. Turing
            LOCATION:CE 2 103
            CATEGORIES:cours
            END:VEVENT
        """

    importer.importFromStream(stream(ics))

    val slot = slot<Class>()
    coVerify { planner.saveClass(capture(slot)) }

    assertEquals("Cours d'info", slot.captured.courseName)
    assertEquals(ClassType.LECTURE, slot.captured.type)
    assertEquals("CE 2 103", slot.captured.location)
    assertEquals("Prof. Turing", slot.captured.instructor)
  }

  @Test
  fun `maps exercise class type`() = runTest {
    val ics =
        """
            BEGIN:VEVENT
            SUMMARY:Exercices maths
            DTSTART:20241201T100000
            CATEGORIES:exercices
            END:VEVENT
        """

    importer.importFromStream(stream(ics))

    val slot = slot<Class>()
    coVerify { planner.saveClass(capture(slot)) }
    assertEquals(ClassType.EXERCISE, slot.captured.type)
  }

  @Test
  fun `blank instructor yields empty string`() = runTest {
    val ics =
        """
            BEGIN:VEVENT
            SUMMARY:Test
            DTSTART:20241201T100000
            DESCRIPTION:
            END:VEVENT
        """

    importer.importFromStream(stream(ics))

    val slot = slot<Class>()
    coVerify { planner.saveClass(capture(slot)) }

    assertEquals("", slot.captured.instructor)
  }
}
