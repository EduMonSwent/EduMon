package com.android.sample.repos_providors

import com.android.sample.feature.homeScreen.HomeRepository
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import com.android.sample.feature.weeks.repository.WeeksRepository
import com.android.sample.model.PlannerRepositoryImpl
import com.android.sample.model.planner.PlannerRepository
import com.android.sample.profile.ProfileRepository
import com.android.sample.repositories.ToDoRepository
import com.android.sample.session.StudySessionRepository
import com.android.sample.ui.stats.repository.StatsRepository

/**
 * Central contract for repository instances used across the app.
 *
 * Implementations live in separate files:
 * - FakeRepositoriesProvider (no Firebase)
 * - FirestoreRepositoriesProvider (Firebase-backed)
 */
interface RepositoriesProvider {
  val objectivesRepository: ObjectivesRepository
  val weeksRepository: WeeksRepository
  val statsRepository: StatsRepository

  // Home / Planner (attendance) / Study Session
  val homeRepository: HomeRepository
  val plannerRepository: PlannerRepository // attendance/class planner
  val studySessionRepository: StudySessionRepository

  // Calendar feature repo (model.calendar.PlannerRepository type)
  val calendarRepository: PlannerRepositoryImpl

  // To-Do
  val toDoRepository: ToDoRepository

  // Profile
  val profileRepository: ProfileRepository
}

/**
 * Global access point used by ViewModels by default. Set this in Application.onCreate() for
 * production, e.g.: AppRepositories = FirestoreRepositoriesProvider
 *
 * Default here is fakes to prevent accidental Firebase init in tests.
 */
