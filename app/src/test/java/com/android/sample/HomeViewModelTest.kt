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
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

  private class TestRepository(
      private val todos: List<ToDo> = run {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        listOf(
            ToDo(
                id = "1",
                title = "A",
                dueDate = today,
                priority = Priority.MEDIUM,
                status = Status.DONE),
            ToDo(id = "2", title = "B", dueDate = today, priority = Priority.LOW),
            ToDo(id = "3", title = "C", dueDate = tomorrow, priority = Priority.HIGH))
      },
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

    var fetchTodosCalled = 0
    var fetchObjectivesCalled = 0
    var fetchCreatureStatsCalled = 0
    var fetchUserStatsCalled = 0
    var dailyQuoteCalled = 0

    override suspend fun fetchTodos(): List<ToDo> {
      fetchTodosCalled++
      return todos
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
        val vm =
            HomeViewModel(
                repository = TestRepository(),
                userStatsRepository = statsRepo,
            )

        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertEquals(3, s.todos.size)
        assertEquals("A", s.todos.first().title)

        // objectives now part of state
        assertEquals(3, s.objectives.size)
        assertEquals("O1", s.objectives.first().title)

        assertEquals(7, s.creatureStats.level)
        assertEquals(99, s.userStats.points)
        assertEquals("Test quote", s.quote)
      }

  @Test
  fun `refresh toggles loading then updates values`() =
      runTest(testDispatcher) {
        val vm =
            HomeViewModel(
                repository = TestRepository(quote = "Q1"), userStatsRepository = statsRepo)

        advanceUntilIdle()
        assertEquals("Q1", vm.uiState.value.quote)

        vm.refresh()
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
      }

  @Test
  fun `supports empty todos`() =
      runTest(testDispatcher) {
        val vm =
            HomeViewModel(
                repository = TestRepository(todos = emptyList(), quote = "EmptyQ"),
                userStatsRepository = statsRepo)

        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.todos.isEmpty())
        assertEquals("EmptyQ", s.quote)
        // objectives still present by default in TestRepository unless overridden
        assertEquals(3, s.objectives.size)
      }

  @Test
  fun `supports empty objectives`() =
      runTest(testDispatcher) {
        val vm =
            HomeViewModel(
                repository = TestRepository(objectives = emptyList(), quote = "Q"),
                userStatsRepository = statsRepo)

        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.objectives.isEmpty())
      }

  @Test
  fun `different repos produce different quotes`() =
      runTest(testDispatcher) {
        val vm1 =
            HomeViewModel(
                repository = TestRepository(quote = "Q1"), userStatsRepository = statsRepo)
        advanceUntilIdle()
        assertEquals("Q1", vm1.uiState.value.quote)

        val vm2 =
            HomeViewModel(
                repository = TestRepository(quote = "Q2"), userStatsRepository = statsRepo)
        advanceUntilIdle()
        assertEquals("Q2", vm2.uiState.value.quote)
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
        assertFalse(statsRepo.startCalled)

        HomeViewModel(repository = TestRepository(), userStatsRepository = statsRepo)

        advanceUntilIdle()
        assertTrue(statsRepo.startCalled)
      }

  @Test
  fun `user stats updates are reflected in UI state`() =
      runTest(testDispatcher) {
        val vm = HomeViewModel(repository = TestRepository(), userStatsRepository = statsRepo)

        advanceUntilIdle()
        assertEquals(99, vm.uiState.value.userStats.points)

        statsRepo.addPoints(51)
        advanceUntilIdle()

        assertEquals(150, vm.uiState.value.userStats.points)
      }

  @Test
  fun `multiple refreshes update state correctly`() =
      runTest(testDispatcher) {
        val repo = TestRepository(quote = "Initial")
        val vm = HomeViewModel(repository = repo, userStatsRepository = statsRepo)

        advanceUntilIdle()
        assertEquals(1, repo.fetchTodosCalled)
        assertEquals(1, repo.fetchObjectivesCalled)
        assertEquals(1, repo.fetchCreatureStatsCalled)

        vm.refresh()
        advanceUntilIdle()

        assertEquals(2, repo.fetchTodosCalled)
        assertEquals(2, repo.fetchObjectivesCalled)
        assertEquals(2, repo.fetchCreatureStatsCalled)

        vm.refresh()
        advanceUntilIdle()

        assertEquals(3, repo.fetchTodosCalled)
        assertEquals(3, repo.fetchObjectivesCalled)
        assertEquals(3, repo.fetchCreatureStatsCalled)
      }

  @Test
  fun `user stats from repository override initial state`() =
      runTest(testDispatcher) {
        val customStats =
            UserStats(
                totalStudyMinutes = 200,
                todayStudyMinutes = 50,
                streak = 10,
                weeklyGoal = 300,
                coins = 100,
                points = 500)
        val customStatsRepo = TestUserStatsRepository(customStats)

        val vm = HomeViewModel(repository = TestRepository(), userStatsRepository = customStatsRepo)

        advanceUntilIdle()

        assertEquals(500, vm.uiState.value.userStats.points)
        assertEquals(10, vm.uiState.value.userStats.streak)
        assertEquals(50, vm.uiState.value.userStats.todayStudyMinutes)
      }

  @Test
  fun `creature stats update independently of user stats`() =
      runTest(testDispatcher) {
        val creature1 = CreatureStats(happiness = 50, health = 60, energy = 70, level = 3)

        val repo1 = TestRepository(creature = creature1)
        val vm = HomeViewModel(repository = repo1, userStatsRepository = statsRepo)

        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.creatureStats.level)
        assertEquals(50, vm.uiState.value.creatureStats.happiness)

        statsRepo.addPoints(100)
        advanceUntilIdle()
        assertEquals(199, vm.uiState.value.userStats.points)

        assertEquals(3, vm.uiState.value.creatureStats.level)
      }

  @Test
  fun `todos maintain original order from repository`() =
      runTest(testDispatcher) {
        val today = LocalDate.now()
        val todos =
            listOf(
                ToDo(id = "3", title = "C", dueDate = today.plusDays(2), priority = Priority.HIGH),
                ToDo(id = "1", title = "A", dueDate = today, priority = Priority.LOW),
                ToDo(
                    id = "2", title = "B", dueDate = today.plusDays(1), priority = Priority.MEDIUM))

        val vm =
            HomeViewModel(
                repository = TestRepository(todos = todos), userStatsRepository = statsRepo)

        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.todos.size)
        assertEquals("C", vm.uiState.value.todos[0].title)
        assertEquals("A", vm.uiState.value.todos[1].title)
        assertEquals("B", vm.uiState.value.todos[2].title)
      }

  @Test
  fun `stats repository updates persist across refreshes`() =
      runTest(testDispatcher) {
        val vm = HomeViewModel(repository = TestRepository(), userStatsRepository = statsRepo)

        advanceUntilIdle()
        assertEquals(99, vm.uiState.value.userStats.points)

        statsRepo.addPoints(100)
        advanceUntilIdle()
        assertEquals(199, vm.uiState.value.userStats.points)

        vm.refresh()
        advanceUntilIdle()

        assertEquals(199, vm.uiState.value.userStats.points)
      }

  @Test
  fun `all user stats fields update correctly`() =
      runTest(testDispatcher) {
        val vm = HomeViewModel(repository = TestRepository(), userStatsRepository = statsRepo)

        advanceUntilIdle()

        statsRepo.addStudyMinutes(30)
        advanceUntilIdle()
        assertEquals(45, vm.uiState.value.userStats.totalStudyMinutes)
        assertEquals(45, vm.uiState.value.userStats.todayStudyMinutes)

        statsRepo.updateCoins(50)
        advanceUntilIdle()
        assertEquals(50, vm.uiState.value.userStats.coins)

        statsRepo.setWeeklyGoal(200)
        advanceUntilIdle()
        assertEquals(200, vm.uiState.value.userStats.weeklyGoal)
      }

  // ---- FakeHomeRepository coverage tests ----

  @Test
  fun fakeRepository_dailyQuote_changesAcrossDays_andWraps() {
    val repo = com.android.sample.feature.homeScreen.FakeHomeRepository()
    val d0 = repo.dailyQuote(nowMillis = 0L)
    val d1 = repo.dailyQuote(nowMillis = 86_400_000L)
    val d6 = repo.dailyQuote(nowMillis = 6L * 86_400_000L)

    assertEquals(d1, d6)
    assertNotEquals(d0, d1)
  }

  @Test
  fun fakeRepository_fetches_haveExpectedSizes() =
      runTest(testDispatcher) {
        val repo = com.android.sample.feature.homeScreen.FakeHomeRepository()
        val todos = repo.fetchTodos()
        val objectives = repo.fetchObjectives()
        val creature = repo.fetchCreatureStats()
        val userProfile = repo.fetchUserStats()

        assertEquals(3, todos.size)
        // objective list size depends on your FakeHomeRepository sample; just ensure it returns a
        // list
        assertNotNull(objectives)
        assertTrue(creature.level >= 1)
        assertEquals("Alex", userProfile.name)
      }
}
