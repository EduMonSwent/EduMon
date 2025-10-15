import com.android.sample.ui.session.StudySessionUiState
import com.android.sample.ui.session.Task

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

interface StudySessionRepository {
  suspend fun saveCompletedSession(session: StudySessionUiState)

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
}
