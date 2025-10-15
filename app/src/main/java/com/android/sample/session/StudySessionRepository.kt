import com.android.sample.ui.session.StudySessionUiState
import com.android.sample.ui.session.Task

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
    return listOf(Task("Read Chapter 1"), Task("Practice Math Problems"), Task("Review Flashcards"))
  }

  /**
   * Retrieves a list of saved study sessions.
   *
   * @return A list of saved study sessions.
   */
  fun getSavedSessions(): List<StudySessionUiState> = savedSessions
}
