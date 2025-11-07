package com.android.sample.session

import com.android.sample.data.Status
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repositories.ToDoRepository
import com.android.sample.ui.session.StudySessionUiState
import com.android.sample.ui.session.Task
import kotlinx.coroutines.flow.first

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

interface StudySessionRepository {
  /**
   * Saves a completed study session.
   *
   * @param session The completed study session to be saved.
   */
  suspend fun saveCompletedSession(session: StudySessionUiState)

  /**
   * Retrieves a list of suggested tasks for the study session.
   *
   * @return A list of suggested tasks.
   */
  suspend fun getSuggestedTasks(): List<Task>
}

class FakeStudySessionRepository : StudySessionRepository { // Temporary Fake implementation
  private val savedSessions = mutableListOf<StudySessionUiState>()

  override suspend fun saveCompletedSession(session: StudySessionUiState) {
    savedSessions.add(session)
  }

  override suspend fun getSuggestedTasks(): List<Task> {
    return listOf()
  }

  /**
   * Retrieves a list of saved study sessions.
   *
   * @return A list of saved study sessions.
   */
  fun getSavedSessions(): List<StudySessionUiState> = savedSessions
}

class ToDoBackedStudySessionRepository(
    private val toDos: ToDoRepository = AppRepositories.toDoRepository
) : StudySessionRepository {

  private val savedSessions = mutableListOf<StudySessionUiState>()

  override suspend fun saveCompletedSession(session: StudySessionUiState) {
    savedSessions.add(session)
  }

  /**
   * Snapshot of suggestions *right now* based on the To-Do repo. It filters out DONE items, sorts
   * by due date, and maps title -> Task(name).
   */
  override suspend fun getSuggestedTasks(): List<Task> {
    val current = toDos.todos.first() // adapt if your flow name differs
    return current
        .asSequence()
        .filter { it.status != Status.DONE }
        .sortedBy { it.dueDate }
        .toList() // type is List<ToDo> == List<Task>
  }
}
