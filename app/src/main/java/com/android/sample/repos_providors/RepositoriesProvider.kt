package com.android.sample.repos_providors

// This code has been written partially using A.I (LLM).

import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.homeScreen.HomeRepository
import com.android.sample.feature.schedule.repository.calendar.CalendarRepositoryImpl
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import com.android.sample.feature.weeks.repository.WeeksRepository
import com.android.sample.profile.ProfileRepository
import com.android.sample.repositories.ToDoRepository
import com.android.sample.session.StudySessionRepository
import com.android.sample.ui.flashcards.data.FlashcardsRepository
import com.android.sample.ui.location.FriendRepository

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

  // Home / Planner (attendance) / Study Session
  val homeRepository: HomeRepository
  val plannerRepository: PlannerRepository // attendance/class planner
  val studySessionRepository: StudySessionRepository

  // Calendar feature repo (model.calendar.PlannerRepository type)
  val calendarRepository: CalendarRepositoryImpl

  // To-Do
  val toDoRepository: ToDoRepository

  val friendRepository: FriendRepository

  // Profile
  val profileRepository: ProfileRepository

  val flashcardsRepository: FlashcardsRepository

  val userStatsRepository: UserStatsRepository
}

/**
 * Global access point used by ViewModels by default. Set this in Application.onCreate() for
 * production, e.g.: AppRepositories = FirestoreRepositoriesProvider
 */
