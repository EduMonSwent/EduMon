// This code was written with the assistance of an AI (LLM).
package com.android.sample.repos_providors

import com.android.sample.data.FakeUserStatsRepository
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.homeScreen.FakeHomeRepository
import com.android.sample.feature.homeScreen.HomeRepository
import com.android.sample.feature.schedule.repository.calendar.CalendarRepositoryImpl
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.repository.schedule.FakeScheduleRepository
import com.android.sample.feature.subjects.repository.FakeSubjectsRepository
import com.android.sample.feature.subjects.repository.SubjectsRepository
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
import com.android.sample.ui.flashcards.data.FlashcardsRepository
import com.android.sample.ui.flashcards.data.InMemoryFlashcardsRepository
import com.android.sample.ui.location.FakeFriendRepository
import com.android.sample.ui.shop.repository.FakeShopRepository
import com.android.sample.ui.shop.repository.ShopRepository
import com.android.sample.ui.stats.repository.FakeStatsRepository
import com.android.sample.ui.stats.repository.StatsRepository

object FakeRepositoriesProvider : RepositoriesProvider {

  private object Defaults {
    const val STARTER_ID = "pyromon"
  }

  override val objectivesRepository: ObjectivesRepository = FakeObjectivesRepository

  override val weeksRepository: WeeksRepository = FakeWeeksRepository()

  override val statsRepository: StatsRepository = FakeStatsRepository()

  override val userStatsRepository: UserStatsRepository = FakeUserStatsRepository()

  override val plannerRepository: PlannerRepository = PlannerRepository()

  override val scheduleRepository = FakeScheduleRepository

  override val studySessionRepository: StudySessionRepository = ToDoBackedStudySessionRepository()

  override val homeRepository: HomeRepository = FakeHomeRepository()

  override val calendarRepository: CalendarRepositoryImpl = CalendarRepositoryImpl()

  override val friendRepository: FakeFriendRepository = FakeFriendRepository()

  override val toDoRepository: ToDoRepository = ToDoRepositoryLocal()

  override val profileRepository: ProfileRepository =
    FakeProfileRepository(initial = UserProfile(starterId = Defaults.STARTER_ID))

  override val flashcardsRepository: FlashcardsRepository = InMemoryFlashcardsRepository

  override val subjectsRepository: SubjectsRepository = FakeSubjectsRepository()

  override val shopRepository: ShopRepository = FakeShopRepository(profileRepository)
}

@Volatile
var FakeRepositories: RepositoriesProvider = FakeRepositoriesProvider