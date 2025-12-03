package com.android.sample.schedule

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.repository.schedule.IcsImporter
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IcsImporterAndroidTest {

  // Fake repositories storing what importer pushes
  class FakeScheduleRepo : ScheduleRepository {
    val saved = mutableListOf<com.android.sample.feature.schedule.data.schedule.ScheduleEvent>()
    override val events = kotlinx.coroutines.flow.MutableStateFlow(saved.toList())

    override suspend fun save(
        event: com.android.sample.feature.schedule.data.schedule.ScheduleEvent
    ) {
      saved += event
    }

    override suspend fun update(
        event: com.android.sample.feature.schedule.data.schedule.ScheduleEvent
    ) {}

    override suspend fun delete(eventId: String) {}

    override suspend fun getEventsBetween(a: java.time.LocalDate, b: java.time.LocalDate) = saved

    override suspend fun getById(id: String) = saved.find { it.id == id }

    override suspend fun moveEventDate(id: String, newDate: java.time.LocalDate) = false

    override suspend fun getEventsForDate(date: java.time.LocalDate) = saved

    override suspend fun getEventsForWeek(startDate: java.time.LocalDate) = saved

    override suspend fun importEvents(
        events: List<com.android.sample.feature.schedule.data.schedule.ScheduleEvent>
    ) {
      saved += events
    }
  }

  class FakePlannerRepo : PlannerRepository() {
    val classes = mutableListOf<com.android.sample.feature.schedule.data.planner.Class>()

    override suspend fun saveClass(
        classItem: com.android.sample.feature.schedule.data.planner.Class
    ): Result<Unit> {
      classes += classItem
      return Result.success(Unit)
    }
  }

  @Test
  fun importer_reads_ics_and_saves_events() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val scheduleRepo = FakeScheduleRepo()
    val plannerRepo = FakePlannerRepo()

    val importer = IcsImporter(scheduleRepo, plannerRepo, context)

    // Provide ICS sample from test assets (you create this file)
    val stream = context.assets.open("sample.ics")

    importer.importFromStream(stream)

    // Validate repository received parsed events/classes
    assertTrue("Expected schedule events", scheduleRepo.saved.isNotEmpty())
    assertTrue("Expected planner classes", plannerRepo.classes.isNotEmpty())
  }
}
