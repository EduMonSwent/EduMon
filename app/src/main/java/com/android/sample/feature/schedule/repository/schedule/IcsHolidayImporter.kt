package com.android.sample.feature.schedule.repository.schedule

import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.Priority
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.data.schedule.SourceTag
import java.io.InputStream

class IcsHolidayImporter(
    private val scheduleRepository: ScheduleRepository,
    private val matcher: KeywordMatcher
) {
  suspend fun importFromStream(stream: InputStream) {
    val events = IcsParser.parse(stream)

    val holidayEvents =
        events
            .filter { it.start == null && it.end == null } // all-day
            .filter { matcher.isHoliday(it.categories + it.title) } // you add this
            .map { ics ->
              ScheduleEvent(
                  title = ics.title,
                  date = ics.date,
                  time = null, // all-day
                  durationMinutes = null,
                  kind = EventKind.ACTIVITY_ASSOCIATION, // or add EventKind.HOLIDAY if you want
                  priority = Priority.MEDIUM,
                  sourceTag = SourceTag.Task,
                  location = ics.location,
                  categories = ics.categories)
            }

    scheduleRepository.importEvents(holidayEvents)
  }
}
