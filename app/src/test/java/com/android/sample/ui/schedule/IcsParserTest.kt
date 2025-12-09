package com.android.sample.ui.schedule

import com.android.sample.feature.schedule.repository.schedule.IcsParser
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class IcsParserTest {

  private fun stream(text: String) = text.trimIndent().byteInputStream()

  @Test
  fun `parses full VEVENT with all fields`() {
    val ics =
        """
            BEGIN:VEVENT
            SUMMARY:Math Lecture
            DTSTART:20241201T101500
            DTEND:20241201T121500
            LOCATION:CE 1 234
            DESCRIPTION:Prof. Newton
            CATEGORIES:cours
            END:VEVENT
        """

    val events = IcsParser.parse(stream(ics))
    val e = events.single()

    assertEquals("Math Lecture", e.title)
    assertEquals(LocalTime.of(10, 15), e.start)
    assertEquals(LocalTime.of(12, 15), e.end)
    assertEquals("CE 1 234", e.location)
    assertEquals("Prof. Newton", e.description)
    assertEquals(listOf("cours"), e.categories)
  }

  @Test
  fun `folded lines are unfolded correctly`() {
    val ics =
        """
            BEGIN:VEVENT
            SUMMARY:Test
            DESCRIPTION:Prof.
             Smith
            DTSTART:20241201T080000
            END:VEVENT
        """

    val ev = IcsParser.parse(stream(ics)).first()
    assertEquals("Prof.Smith", ev.description)
  }

  @Test
  fun `weekly RRULE expands events`() {
    val ics =
        """
            BEGIN:VEVENT
            SUMMARY:Weekly class
            DTSTART:20241201T080000
            RRULE:FREQ=WEEKLY;UNTIL=20241215
            END:VEVENT
        """

    val events = IcsParser.parse(stream(ics))
    // 1 Dec, 8 Dec, 15 Dec
    assertEquals(3, events.size)
  }
}
