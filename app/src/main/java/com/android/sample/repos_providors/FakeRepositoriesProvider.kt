package com.android.sample.repos_providors

import com.android.sample.feature.homeScreen.FakeHomeRepository
import com.android.sample.feature.homeScreen.HomeRepository
import com.android.sample.feature.schedule.repository.calendar.CalendarRepositoryImpl
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.feature.weeks.repository.FakeWeeksRepository
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import com.android.sample.feature.weeks.repository.WeeksRepository
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.profile.ProfileRepository
import com.android.sample.repositories.ToDoRepository
import com.android.sample.repositories.ToDoRepositoryLocal
import com.android.sample.session.StudySessionRepository
import com.android.sample.session.ToDoBackedStudySessionRepository
import com.android.sample.ui.stats.repository.FakeStatsRepository
import com.android.sample.ui.stats.repository.StatsRepository

/** Provider of in-memory fake repositories (no Firebase). */
object FakeRepositoriesProvider : RepositoriesProvider {
  override val objectivesRepository: ObjectivesRepository = FakeObjectivesRepository
  override val weeksRepository: WeeksRepository = FakeWeeksRepository()
  override val statsRepository: StatsRepository = FakeStatsRepository()

  override val plannerRepository: PlannerRepository = PlannerRepository()
  override val studySessionRepository: StudySessionRepository = ToDoBackedStudySessionRepository()
  override val homeRepository: HomeRepository = FakeHomeRepository()

  override val calendarRepository: CalendarRepositoryImpl = CalendarRepositoryImpl()

  override val toDoRepository: ToDoRepository = ToDoRepositoryLocal()
  override val profileRepository: ProfileRepository = FakeProfileRepository()
}

@Volatile var FakeRepositories: RepositoriesProvider = FakeRepositoriesProvider
