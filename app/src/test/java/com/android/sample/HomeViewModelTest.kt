package com.android.sample

import com.android.sample.data.CreatureStats
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.homeScreen.HomeRepository
import com.android.sample.feature.homeScreen.HomeUiState
import com.android.sample.feature.homeScreen.HomeViewModel
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.repositories.ToDoRepository
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

  /** Simple fake UserStatsRepository for unit tests. */
  private class TestUserStatsRepository(initial: UserStats = UserStats()) : UserStatsRepository {
    private val _stats = MutableStateFlow(initial)
    override val stats: StateFlow<UserStats> = _stats

    var startCalled = false

    override suspend fun start() {
      startCalled = true
    }

    override suspend fun addStudyMinutes(extraMinutes: Int) {
      _stats.value =
          _stats.value.copy(
              totalStudyMinutes = _stats.value.totalStudyMinutes + extraMinutes,
              todayStudyMinutes = _stats.value.todayStudyMinutes + extraMinutes,
          )
    }

    override suspend fun addPoints(delta: Int) {
      _stats.value = _stats.value.copy(points = _stats.value.points + delta)
    }

    override suspend fun updateCoins(delta: Int) {
      _stats.value = _stats.value.copy(coins = _stats.value.coins + delta)
    }

    override suspend fun setWeeklyGoal(goalMinutes: Int) {
      _stats.value = _stats.value.copy(weeklyGoal = goalMinutes)
    }
  }

  private class TestToDoRepository(initialTodos: List<ToDo>) : ToDoRepository {
    private val _todos = MutableStateFlow(initialTodos)
    override val todos: StateFlow<List<ToDo>> = _todos.asStateFlow()

    override suspend fun add(todo: ToDo) {
      TODO("Not yet implemented")
    }

    override suspend fun update(todo: ToDo) {
      TODO("Not yet implemented")
    }

    override suspend fun remove(id: String) {
      TODO("Not yet implemented")
    }

    override suspend fun getById(id: String): ToDo? {
      TODO("Not yet implemented")
    }

    fun emit(newTodos: List<ToDo>) {
      _todos.value = newTodos
    }
  }

  private class TestRepository(
      private val objectives: List<Objective> =
          listOf(
              Objective(title = "O1", course = "CS-101", day = DayOfWeek.MONDAY, completed = false),
              Objective(
                  title = "O2", course = "Math", day = DayOfWeek.WEDNESDAY, completed = false),
              Objective(title = "O3", course = "Career", day = DayOfWeek.FRIDAY, completed = true),
          ),
      private val creature: CreatureStats =
          CreatureStats(happiness = 10, health = 20, energy = 30, level = 7),
      private val quote: String = "Test quote"
  ) : HomeRepository {

    var fetchObjectivesCalled = 0
    var fetchCreatureStatsCalled = 0
    var fetchUserStatsCalled = 0
    var dailyQuoteCalled = 0
    var fetchTodosCalled = 0

    // HomeViewModel should not call this anymore once you switch to ToDoRepository Flow.
    override suspend fun fetchTodos(): List<ToDo> {
      fetchTodosCalled++
      return emptyList()
    }

    override suspend fun fetchObjectives(): List<Objective> {
      fetchObjectivesCalled++
      return objectives
    }

    override suspend fun fetchCreatureStats(): CreatureStats {
      fetchCreatureStatsCalled++
      return creature
    }

    override suspend fun fetchUserStats(): UserProfile {
      fetchUserStatsCalled++
      return UserProfile()
    }

    override fun dailyQuote(nowMillis: Long): String {
      dailyQuoteCalled++
      return quote
    }
  }

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var statsRepo: TestUserStatsRepository

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    statsRepo =
        TestUserStatsRepository(
            UserStats(
                totalStudyMinutes = 15,
                todayStudyMinutes = 15,
                streak = 3,
                weeklyGoal = 120,
                coins = 0,
                points = 99))
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial refresh populates state and stops loading`() =
      runTest(testDispatcher) {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val initialTodos =
            listOf(
                ToDo(
                    id = "1",
                    title = "A",
                    dueDate = today,
                    priority = Priority.MEDIUM,
                    status = Status.DONE),
                ToDo(id = "2", title = "B", dueDate = today, priority = Priority.LOW),
                ToDo(id = "3", title = "C", dueDate = tomorrow, priority = Priority.HIGH),
            )

        val toDoRepo = TestToDoRepository(initialTodos)
        val homeRepo = TestRepository()

        val vm =
            HomeViewModel(
                repository = homeRepo,
                userStatsRepository = statsRepo,
                toDoRepository = toDoRepo,
            )

        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.isLoading)

        // ✅ todos now come from ToDoRepository Flow
        assertEquals(3, s.todos.size)
        assertEquals("A", s.todos.first().title)

        // objectives still come from HomeRepository
        assertEquals(3, s.objectives.size)
        assertEquals("O1", s.objectives.first().title)

        assertEquals(7, s.creatureStats.level)
        assertEquals(99, s.userStats.points)
        assertEquals("Test quote", s.quote)

        // ✅ ensure HomeRepository.fetchTodos is no longer used
        assertEquals(0, homeRepo.fetchTodosCalled)
      }

  @Test
  fun `refresh toggles loading then updates values`() =
      runTest(testDispatcher) {
        val toDoRepo = TestToDoRepository(emptyList())
        val homeRepo = TestRepository(quote = "Q1")

        val vm =
            HomeViewModel(
                repository = homeRepo,
                userStatsRepository = statsRepo,
                toDoRepository = toDoRepo,
            )

        advanceUntilIdle()
        assertEquals("Q1", vm.uiState.value.quote)

        vm.refresh()
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)

        // still should not call fetchTodos
        assertEquals(0, homeRepo.fetchTodosCalled)
      }

  @Test
  fun `supports empty todos`() =
      runTest(testDispatcher) {
        val toDoRepo = TestToDoRepository(emptyList())
        val homeRepo = TestRepository(quote = "EmptyQ")

        val vm =
            HomeViewModel(
                repository = homeRepo,
                userStatsRepository = statsRepo,
                toDoRepository = toDoRepo,
            )

        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.todos.isEmpty())
        assertEquals("EmptyQ", s.quote)
        assertEquals(3, s.objectives.size)
      }

  @Test
  fun `supports empty objectives`() =
      runTest(testDispatcher) {
        val toDoRepo = TestToDoRepository(emptyList())
        val homeRepo = TestRepository(objectives = emptyList(), quote = "Q")

        val vm =
            HomeViewModel(
                repository = homeRepo,
                userStatsRepository = statsRepo,
                toDoRepository = toDoRepo,
            )

        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.objectives.isEmpty())
      }

  @Test
  fun `homeUiState defaults are sane`() {
    val s = HomeUiState()
    assertTrue(s.isLoading)
    assertTrue(s.todos.isEmpty())
    assertTrue(s.objectives.isEmpty())
    assertTrue(s.creatureStats.level >= 1)
    assertEquals(UserStats(), s.userStats)
    assertEquals("", s.quote)
  }

  @Test
  fun `userStatsRepository start is called on init`() =
      runTest(testDispatcher) {
        val toDoRepo = TestToDoRepository(emptyList())
        val homeRepo = TestRepository()

        assertFalse(statsRepo.startCalled)

        HomeViewModel(
            repository = homeRepo, userStatsRepository = statsRepo, toDoRepository = toDoRepo)

        advanceUntilIdle()
        assertTrue(statsRepo.startCalled)
      }

  @Test
  fun `user stats updates are reflected in UI state`() =
      runTest(testDispatcher) {
        val toDoRepo = TestToDoRepository(emptyList())
        val homeRepo = TestRepository()

        val vm =
            HomeViewModel(
                repository = homeRepo,
                userStatsRepository = statsRepo,
                toDoRepository = toDoRepo,
            )

        advanceUntilIdle()
        assertEquals(99, vm.uiState.value.userStats.points)

        statsRepo.addPoints(51)
        advanceUntilIdle()

        assertEquals(150, vm.uiState.value.userStats.points)
      }

  @Test
  fun `todos update live when ToDoRepository emits new list`() =
      runTest(testDispatcher) {
        val today = LocalDate.now()
        val toDoRepo = TestToDoRepository(emptyList())
        val homeRepo = TestRepository()

        val vm =
            HomeViewModel(
                repository = homeRepo,
                userStatsRepository = statsRepo,
                toDoRepository = toDoRepo,
            )

        advanceUntilIdle()
        assertTrue(vm.uiState.value.todos.isEmpty())

        val newTodos =
            listOf(
                ToDo(id = "x", title = "New 1", dueDate = today, priority = Priority.LOW),
                ToDo(id = "y", title = "New 2", dueDate = today, priority = Priority.HIGH),
            )

        toDoRepo.emit(newTodos)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.todos.size)
        assertEquals("New 1", vm.uiState.value.todos[0].title)
        assertEquals("New 2", vm.uiState.value.todos[1].title)
      }

  @Test
  fun `fakeRepository_dailyQuote_changesAcrossDays_andWraps`() {
    val repo = com.android.sample.feature.homeScreen.FakeHomeRepository()
    val d0 = repo.dailyQuote(nowMillis = 0L)
    val d1 = repo.dailyQuote(nowMillis = 86_400_000L)
    val d6 = repo.dailyQuote(nowMillis = 6L * 86_400_000L)

    assertEquals(d1, d6)
    assertNotEquals(d0, d1)
  }
}
