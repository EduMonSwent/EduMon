package com.android.sample.repos_providors

// This code has been written partially using A.I (LLM).

import com.android.sample.data.FirestoreUserStatsRepository
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.homeScreen.FakeHomeRepository
import com.android.sample.feature.homeScreen.HomeRepository
import com.android.sample.feature.schedule.repository.calendar.CalendarRepositoryImpl
import com.android.sample.feature.schedule.repository.planner.PlannerRepository as PlannerRepoForPlanner
import com.android.sample.feature.weeks.repository.FirestoreObjectivesRepository
import com.android.sample.feature.weeks.repository.FirestoreWeeksRepository
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import com.android.sample.feature.weeks.repository.WeeksRepository
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.profile.ProfileRepository
import com.android.sample.repositories.ToDoRepository
import com.android.sample.repositories.ToDoRepositoryLocal
import com.android.sample.session.StudySessionRepository
import com.android.sample.session.ToDoBackedStudySessionRepository
import com.android.sample.ui.flashcards.data.FirestoreFlashcardsRepository
import com.android.sample.ui.flashcards.data.FlashcardsRepository
import com.android.sample.ui.location.FriendRepository
import com.android.sample.ui.location.ProfilesFriendRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/** Firebase-backed provider with LAZY init to prevent accidental Firebase loading. */
object FirestoreRepositoriesProvider : RepositoriesProvider {

  private val db by lazy { Firebase.firestore }
  private val auth by lazy { Firebase.auth }

  override val objectivesRepository: ObjectivesRepository by lazy {
    FirestoreObjectivesRepository(db, auth)
  }

  override val weeksRepository: WeeksRepository by lazy { FirestoreWeeksRepository(db, auth) }

  // StatsRepository removed

  override val friendRepository: FriendRepository by lazy { ProfilesFriendRepository(db, auth) }

  override val flashcardsRepository: FlashcardsRepository by lazy {
    FirestoreFlashcardsRepository(db, auth)
  }

  // Local implementations until remote backends exist
  override val homeRepository: HomeRepository by lazy { FakeHomeRepository() }

  override val plannerRepository: PlannerRepoForPlanner by lazy { PlannerRepoForPlanner() }

  override val studySessionRepository: StudySessionRepository by lazy {
    ToDoBackedStudySessionRepository()
  }

  override val calendarRepository: CalendarRepositoryImpl by lazy { CalendarRepositoryImpl() }

  override val toDoRepository: ToDoRepository by lazy { ToDoRepositoryLocal() }

  override val profileRepository: ProfileRepository by lazy { FakeProfileRepository() }

  override val userStatsRepository: UserStatsRepository by lazy {
    // FirestoreUserStatsRepository(auth, firestore) – unified stats document
    FirestoreUserStatsRepository(auth, db)
  }
}

@Volatile var AppRepositories: RepositoriesProvider = FirestoreRepositoriesProvider
