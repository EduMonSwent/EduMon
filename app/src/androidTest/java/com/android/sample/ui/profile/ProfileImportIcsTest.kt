package com.android.sample.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.repository.schedule.IcsImporter
import com.android.sample.schedule.IcsImporterAndroidTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ProfileImportIcsTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun importer_reads_ics_and_saves_classes_only() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val scheduleRepo = IcsImporterAndroidTest.FakeScheduleRepo()
    val plannerRepo = IcsImporterAndroidTest.FakePlannerRepo()

    val importer = IcsImporter(scheduleRepo, plannerRepo, context)

    val ics =
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        SUMMARY:Imported Class
        DTSTART:20250302T140000
        DTEND:20250302T160000
        DESCRIPTION:Prof
        LOCATION:Room A
        END:VEVENT
        END:VCALENDAR
    """
            .trimIndent()

    val stream = ics.byteInputStream()
    importer.importFromStream(stream)

    // EXPECTATION: Only plannerRepo receives data
    assertTrue("Expected planner classes", plannerRepo.classes.isNotEmpty())
    assertEquals("Imported Class", plannerRepo.classes.first().courseName)

    // Schedule repo should remain empty because importer does not save events
    assertTrue("Schedule events should remain empty", scheduleRepo.saved.isEmpty())
  }
}
