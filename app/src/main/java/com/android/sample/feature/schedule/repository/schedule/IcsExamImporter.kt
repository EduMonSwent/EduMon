package com.android.sample.feature.schedule.repository.schedule

import com.android.sample.feature.schedule.data.schedule.*
import java.io.InputStream
import kotlinx.coroutines.flow.first

class IcsExamImporter(
    private val scheduleRepository: ScheduleRepository,
    private val matcher: KeywordMatcher
) {

  suspend fun importFromStream(stream: InputStream) {
    val existingEvents = scheduleRepository.events.first()

    existingEvents
        .filter { it.kind == EventKind.EXAM_FINAL || it.kind == EventKind.EXAM_MIDTERM }
        .forEach { scheduleRepository.delete(it.id) }
    val events = IcsParser.parse(stream)

    val examEvents =
        events
            .filter { matcher.isExam(it.categories) }
            .map { ics ->
              val stableId = "exam:${ics.title}:${ics.date}"

              ScheduleEvent(
                  id = stableId,
                  title = ics.title,
                  date = ics.date,
                  time = ics.start,
                  durationMinutes =
                      if (ics.start != null && ics.end != null)
                          java.time.Duration.between(ics.start, ics.end).toMinutes().toInt()
                      else null,
                  kind = mapExamKind(ics.categories),
                  priority = Priority.HIGH,
                  sourceTag = SourceTag.Task,
                  location = ics.location,
                  categories = ics.categories)
            }

    scheduleRepository.importEvents(examEvents)
  }

  private fun mapExamKind(categories: List<String>): EventKind {
    val all = categories.joinToString(" ").lowercase()

    return when {
      "midterm" in all -> EventKind.EXAM_MIDTERM
      "written" in all -> EventKind.EXAM_FINAL
      else -> EventKind.EXAM_FINAL
    }
  }
}
