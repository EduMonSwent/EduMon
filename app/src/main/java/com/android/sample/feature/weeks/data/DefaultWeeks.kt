package com.android.sample.feature.weeks.data

import com.android.sample.feature.weeks.model.CourseMaterial
import com.android.sample.feature.weeks.model.DayStatus
import com.android.sample.feature.weeks.model.Exercise
import com.android.sample.feature.weeks.model.WeekContent
import com.android.sample.feature.weeks.model.WeekProgressItem
import java.time.DayOfWeek

/**
 * Central place for hardcoded defaults used by previews, unsigned sessions, local fakes and
 * Firestore seed. Keeping them here prevents duplication and keeps tests consistent.
 */
object DefaultWeeksProvider {

  /** Default weeks content. Percents are nominal; callers may recompute. */
  fun provideDefaultWeeks(): List<WeekProgressItem> =
      listOf(
          WeekProgressItem(
              label = "Week 1",
              percent = 100,
              content =
                  WeekContent(
                      courses =
                          listOf(
                              CourseMaterial(id = "c1", title = "Intro to Android", read = true),
                              CourseMaterial(id = "c2", title = "Compose Basics", read = true),
                          ),
                      exercises =
                          listOf(
                              Exercise(id = "e1", title = "Set up environment", done = true),
                              Exercise(id = "e2", title = "Finish codelab", done = true),
                          ))),
          WeekProgressItem(
              label = "Week 2",
              percent = 55,
              content =
                  WeekContent(
                      courses =
                          listOf(
                              CourseMaterial(id = "c3", title = "Compose Layouts", read = false),
                              CourseMaterial(
                                  id = "c4", title = "State and Side-effects", read = true),
                          ),
                      exercises =
                          listOf(
                              Exercise(id = "e3", title = "Build layout challenge", done = false),
                          ))),
          WeekProgressItem(
              label = "Week 3",
              percent = 10,
              content =
                  WeekContent(
                      courses =
                          listOf(
                              CourseMaterial(
                                  id = "c5", title = "Architecture guidance", read = false),
                          ),
                      exercises =
                          listOf(
                              Exercise(
                                  id = "e4", title = "Repository implementation", done = false),
                          ))),
      )

  /** Default week dots status used in previews/unsigned sessions. */
  fun provideDefaultDayStatuses(): List<DayStatus> =
      DayOfWeek.values().mapIndexed { idx, d -> DayStatus(d, metTarget = idx % 2 == 0) }
}
