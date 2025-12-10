package com.android.sample.feature.schedule.repository.schedule

import android.content.Context
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import java.io.InputStream
import java.time.LocalTime
import java.util.UUID

/** This class was implemented with the help of ai (ChatGPT) */
class IcsImporter(private val plannerRepository: PlannerRepository, context: Context) {
  private val matcher = KeywordMatcher(context)

  suspend fun importFromStream(stream: InputStream) {
    plannerRepository.clearClasses()
    val classes = IcsParser.parse(stream)
    val groupedClasses = classes.groupBy { CourseIdentity(it.title, it.start, it.end, it.location) }

    for ((c, events) in groupedClasses) {
      val representative = events.first()
      // Skip exam events
      if (matcher.isExam(representative.categories)) continue
      val daysOfWeek = events.map { it.date.dayOfWeek }.distinct()

      val classItem =
          Class(
              id = UUID.randomUUID().toString(),
              courseName = c.title.trim(),
              startTime = c.start ?: LocalTime.NOON,
              endTime = c.end ?: c.start?.plusHours(1) ?: LocalTime.NOON.plusHours(1),
              type = mapClassType(representative),
              location = c.location ?: "",
              instructor = extractInstructor(representative.description),
              daysOfWeek = daysOfWeek)
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

  private data class CourseIdentity(
      val title: String,
      val start: LocalTime?,
      val end: LocalTime?,
      val location: String?
  )

  private fun extractInstructor(description: String?): String {
    if (description.isNullOrBlank()) return ""
    var instructor = description.trim()
    return instructor
  }
}
