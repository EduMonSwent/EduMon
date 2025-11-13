package com.android.sample.repos_providors

import com.android.sample.feature.homeScreen.FakeHomeRepository
import com.android.sample.feature.homeScreen.HomeRepository
import com.android.sample.feature.weeks.repository.FirestoreObjectivesRepository
import com.android.sample.feature.weeks.repository.FirestoreWeeksRepository
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import com.android.sample.feature.weeks.repository.WeeksRepository
import com.android.sample.model.PlannerRepositoryImpl
import com.android.sample.model.planner.PlannerRepository as PlannerRepoForPlanner
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.profile.ProfileRepository
import com.android.sample.repositories.ToDoRepository
import com.android.sample.repositories.ToDoRepositoryLocal
import com.android.sample.session.StudySessionRepository
import com.android.sample.session.ToDoBackedStudySessionRepository
import com.android.sample.ui.location.FriendRepository
import com.android.sample.ui.location.ProfilesFriendRepository
import com.android.sample.ui.stats.repository.FirestoreStatsRepository
import com.android.sample.ui.stats.repository.StatsRepository
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
  override val statsRepository: StatsRepository by lazy { FirestoreStatsRepository(db, auth) }

  override val friendRepository: FriendRepository by lazy { ProfilesFriendRepository(db, auth) }

  // Local implementations until remote backends exist
  override val homeRepository: HomeRepository by lazy { FakeHomeRepository() }
  override val plannerRepository: PlannerRepoForPlanner by lazy { PlannerRepoForPlanner() }
  override val studySessionRepository: StudySessionRepository by lazy {
    ToDoBackedStudySessionRepository()
  }

  override val calendarRepository: PlannerRepositoryImpl by lazy { PlannerRepositoryImpl() }

  override val toDoRepository: ToDoRepository by lazy { ToDoRepositoryLocal() }
  override val profileRepository: ProfileRepository by lazy { FakeProfileRepository() }
}

@Volatile var AppRepositories: RepositoriesProvider = FirestoreRepositoriesProvider
