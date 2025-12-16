// This code was written with the assistance of an AI (LLM).
package com.android.sample.repos_providors

import com.android.sample.data.FirestoreUserStatsRepository
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.homeScreen.FakeHomeRepository
import com.android.sample.feature.homeScreen.HomeRepository
import com.android.sample.feature.schedule.repository.calendar.CalendarRepositoryImpl
import com.android.sample.feature.schedule.repository.planner.FirestorePlannerRepository
import com.android.sample.feature.schedule.repository.planner.PlannerRepository as PlannerRepoForPlanner
import com.android.sample.feature.schedule.repository.schedule.FirestoreScheduleRepository
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import com.android.sample.feature.subjects.repository.FirestoreSubjectsRepository
import com.android.sample.feature.subjects.repository.SubjectsRepository
import com.android.sample.feature.weeks.repository.FirestoreObjectivesRepository
import com.android.sample.feature.weeks.repository.FirestoreWeeksRepository
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import com.android.sample.feature.weeks.repository.WeeksRepository
import com.android.sample.profile.FirestoreProfileRepository
import com.android.sample.profile.ProfileRepository
import com.android.sample.repositories.ToDoRepository
import com.android.sample.repositories.ToDoRepositoryLocal
import com.android.sample.session.StudySessionRepository
import com.android.sample.session.ToDoBackedStudySessionRepository
import com.android.sample.ui.flashcards.data.FirestoreFlashcardsRepository
import com.android.sample.ui.flashcards.data.FlashcardsRepository
import com.android.sample.ui.location.FriendRepository
import com.android.sample.ui.location.ProfilesFriendRepository
import com.android.sample.ui.shop.repository.FirestoreShopRepository
import com.android.sample.ui.shop.repository.ShopRepository
import com.android.sample.ui.stats.repository.FirestoreStatsRepository
import com.android.sample.ui.stats.repository.StatsRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FirestoreRepositoriesProvider : RepositoriesProvider {

  private val db by lazy { Firebase.firestore }
  private val auth by lazy { Firebase.auth }

  override val objectivesRepository: ObjectivesRepository by lazy {
    FirestoreObjectivesRepository(db, auth)
  }

  override val weeksRepository: WeeksRepository by lazy {
    FirestoreWeeksRepository(db, auth)
  }

  override val statsRepository: StatsRepository by lazy {
    FirestoreStatsRepository(db, auth)
  }

  override val userStatsRepository: UserStatsRepository by lazy {
    FirestoreUserStatsRepository(auth, db)
  }

  override val friendRepository: FriendRepository by lazy {
    ProfilesFriendRepository(db, auth)
  }

  override val flashcardsRepository: FlashcardsRepository by lazy {
    FirestoreFlashcardsRepository(db, auth)
  }

  override val scheduleRepository: ScheduleRepository by lazy {
    FirestoreScheduleRepository(db, auth)
  }

  override val plannerRepository: PlannerRepoForPlanner by lazy {
    FirestorePlannerRepository(db, auth)
  }

  override val homeRepository: HomeRepository by lazy {
    FakeHomeRepository()
  }

  override val studySessionRepository: StudySessionRepository by lazy {
    ToDoBackedStudySessionRepository()
  }

  override val calendarRepository: CalendarRepositoryImpl by lazy {
    CalendarRepositoryImpl()
  }

  override val toDoRepository: ToDoRepository by lazy {
    ToDoRepositoryLocal()
  }

  override val profileRepository: ProfileRepository by lazy {
    FirestoreProfileRepository(db, auth)
  }

  override val subjectsRepository: SubjectsRepository by lazy {
    FirestoreSubjectsRepository(auth, db)
  }

  override val shopRepository: ShopRepository by lazy {
    FirestoreShopRepository(db, auth, profileRepository)
  }
}

@Volatile
var AppRepositories: RepositoriesProvider = FirestoreRepositoriesProvider