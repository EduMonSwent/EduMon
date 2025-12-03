package com.android.sample.feature.schedule.repository.schedule

import android.content.Context
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import java.io.InputStream
import java.time.LocalTime
import java.util.UUID

class IcsImporter(
    private val scheduleRepository: ScheduleRepository,
    private val plannerRepository: PlannerRepository,
    context: Context
) {
  private val matcher = KeywordMatcher(context)

  suspend fun importFromStream(stream: InputStream) {
    val classes = IcsParser.parse(stream)

    for (c in classes) {
      // Skip exam events
      if (matcher.isExam(c.categories)) continue

      val classItem =
          Class(
              id = UUID.randomUUID().toString(),
              courseName = c.title.trim(),
              startTime = c.start ?: LocalTime.NOON,
              endTime = c.end ?: c.start?.plusHours(1) ?: LocalTime.NOON.plusHours(1),
              type = mapClassType(c),
              location = c.location ?: "",
              instructor = extractInstructor(c.description))
      plannerRepository.saveClass(classItem)
    }
  }

  private fun mapClassType(c: IcsParser.IcsClass): ClassType {
    val all = (c.categories + c.title).joinToString(" ")

    return when {
      matcher.isExercise(all) -> ClassType.EXERCISE
      matcher.isLab(all) -> ClassType.LAB
      matcher.isProject(all) -> ClassType.PROJECT
      matcher.isLecture(all) -> ClassType.LECTURE
      else -> ClassType.LECTURE
    }
  }

  private fun extractInstructor(description: String?): String {
    if (description.isNullOrBlank()) return ""
    var instructor = description.trim()
    return instructor
  }
}
