package com.android.sample.ui.schedule

import com.android.sample.feature.schedule.data.schedule.*
import com.android.sample.feature.schedule.repository.schedule.IcsExamImporter
import com.android.sample.feature.schedule.repository.schedule.KeywordMatcher
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import io.mockk.*
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class IcsExamImporterTest {

  private lateinit var scheduleRepository: ScheduleRepository
  private lateinit var matcher: KeywordMatcher
  private lateinit var importer: IcsExamImporter

  private val existingEventsFlow =
      MutableStateFlow(
          listOf(
              ScheduleEvent(
                  id = "old-exam",
                  title = "Old Exam",
                  date = java.time.LocalDate.of(2025, 1, 1),
                  kind = EventKind.EXAM_FINAL,
                  sourceTag = SourceTag.Task)))

  @Before
  fun setup() {
    scheduleRepository = mockk(relaxed = true)
    matcher = mockk()

    every { scheduleRepository.events } returns existingEventsFlow
    every { matcher.isExam(any()) } returns true

    importer = IcsExamImporter(scheduleRepository, matcher)
  }

  @Test
  fun `old exams are deleted and new exams imported`() = runBlocking {
    val ics =
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        SUMMARY:New Exam
        DTSTART:20250110T090000
        DTEND:20250110T110000
        CATEGORIES:Written
        END:VEVENT
        END:VCALENDAR
        """
            .trimIndent()

    importer.importFromStream(ByteArrayInputStream(ics.toByteArray()))

    coVerify(exactly = 1) { scheduleRepository.delete("old-exam") }

    val slot = slot<List<ScheduleEvent>>()
    coVerify(exactly = 1) { scheduleRepository.importEvents(capture(slot)) }

    val imported = slot.captured.single()
    assertEquals("New Exam", imported.title)
    assertEquals(EventKind.EXAM_FINAL, imported.kind)
    assertEquals(Priority.HIGH, imported.priority)
  }
}
