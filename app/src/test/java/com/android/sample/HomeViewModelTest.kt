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

@Deprecated("This test class should not be used since we removed the creature stats")
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
      // creature param is now effectively ignored for stats logic, but kept for compatibility if
      // needed or removed
      private val creature: CreatureStats = CreatureStats(),
      private val quote: String = "Test quote",
      private val userProfile: UserProfile = UserProfile(level = 7)
  ) : HomeRepository {

    var fetchTodosCalled = 0
    var fetchCreatureStatsCalled = 0
    var fetchUserStatsCalled = 0
    var dailyQuoteCalled = 0

    override suspend fun fetchTodos(): List<ToDo> {
      fetchTodosCalled++
      return todos
    }

    override suspend fun fetchCreatureStats(): CreatureStats {
      fetchCreatureStatsCalled++
      return creature
    }

    override suspend fun fetchUserStats(): UserProfile {
      fetchUserStatsCalled++
      return userProfile
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
  fun `initial refresh populates state and stops loading`() = runTest {
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
    // Now checking userLevel directly
    assertEquals(7, s.userLevel)
    assertEquals(99, s.userStats.points)
    assertEquals("Test quote", s.quote)
  }

  @Test
  fun `refresh toggles loading then updates values`() = runTest {
    val vm =
        HomeViewModel(repository = TestRepository(quote = "Q1"), userStatsRepository = statsRepo)

    advanceUntilIdle()
    assertEquals("Q1", vm.uiState.value.quote)

    vm.refresh()
    assertTrue(vm.uiState.value.isLoading)
    advanceUntilIdle()
    assertFalse(vm.uiState.value.isLoading)
  }

  @Test
  fun `supports empty todos`() = runTest {
    val vm =
        HomeViewModel(
            repository = TestRepository(todos = emptyList(), quote = "EmptyQ"),
            userStatsRepository = statsRepo)

    advanceUntilIdle()

    val s = vm.uiState.value
    assertTrue(s.todos.isEmpty())
    assertEquals("EmptyQ", s.quote)
  }

  @Test
  fun `different repos produce different quotes`() = runTest {
    val vm1 =
        HomeViewModel(repository = TestRepository(quote = "Q1"), userStatsRepository = statsRepo)
    advanceUntilIdle()
    assertEquals("Q1", vm1.uiState.value.quote)

    val vm2 =
        HomeViewModel(repository = TestRepository(quote = "Q2"), userStatsRepository = statsRepo)
    advanceUntilIdle()
    assertEquals("Q2", vm2.uiState.value.quote)
  }

  @Test
  fun `homeUiState defaults are sane`() {
    val s = HomeUiState()
    assertTrue(s.isLoading)
    assertTrue(s.todos.isEmpty())
    // userLevel default is 1
    assertEquals(1, s.userLevel)
    assertEquals(UserStats(), s.userStats)
    assertEquals("", s.quote)
  }

  @Test
  fun `userStatsRepository start is called on init`() = runTest {
    assertFalse(statsRepo.startCalled)

    HomeViewModel(repository = TestRepository(), userStatsRepository = statsRepo)

    advanceUntilIdle()
    assertTrue(statsRepo.startCalled)
  }

  @Test
  fun `user stats updates are reflected in UI state`() = runTest {
    val vm = HomeViewModel(repository = TestRepository(), userStatsRepository = statsRepo)

    advanceUntilIdle()
    assertEquals(99, vm.uiState.value.userStats.points)

    statsRepo.addPoints(51)
    advanceUntilIdle()

    assertEquals(150, vm.uiState.value.userStats.points)
  }

  @Test
  fun `multiple refreshes update state correctly`() = runTest {
    val repo = TestRepository(quote = "Initial")
    val vm = HomeViewModel(repository = repo, userStatsRepository = statsRepo)

    advanceUntilIdle()
    assertEquals(1, repo.fetchTodosCalled)

    assertEquals(0, repo.fetchCreatureStatsCalled)
    assertEquals(1, repo.fetchUserStatsCalled)

    vm.refresh()
    advanceUntilIdle()

    assertEquals(2, repo.fetchTodosCalled)
    assertEquals(0, repo.fetchCreatureStatsCalled)
    assertEquals(2, repo.fetchUserStatsCalled)

    vm.refresh()
    advanceUntilIdle()

    assertEquals(3, repo.fetchTodosCalled)
    assertEquals(0, repo.fetchCreatureStatsCalled)
    assertEquals(3, repo.fetchUserStatsCalled)
  }

  @Test
  fun `user stats from repository override initial state`() = runTest {
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
  fun `user level comes from user profile fetch`() = runTest {
    val profile = UserProfile(level = 42)
    val repo1 = TestRepository(userProfile = profile)
    val vm = HomeViewModel(repository = repo1, userStatsRepository = statsRepo)

    advanceUntilIdle()
    assertEquals(42, vm.uiState.value.userLevel)

    statsRepo.addPoints(100)
    advanceUntilIdle()
    assertEquals(199, vm.uiState.value.userStats.points)

    // Level stays 42 until another refresh or logic update (logic is handled in ProfileViewModel
    // mostly)
    assertEquals(42, vm.uiState.value.userLevel)
  }

  @Test
  fun `todos maintain original order from repository`() = runTest {
    val today = LocalDate.now()
    val todos =
        listOf(
            ToDo(id = "3", title = "C", dueDate = today.plusDays(2), priority = Priority.HIGH),
            ToDo(id = "1", title = "A", dueDate = today, priority = Priority.LOW),
            ToDo(id = "2", title = "B", dueDate = today.plusDays(1), priority = Priority.MEDIUM))

    val vm =
        HomeViewModel(repository = TestRepository(todos = todos), userStatsRepository = statsRepo)

    advanceUntilIdle()

    assertEquals(3, vm.uiState.value.todos.size)
    assertEquals("C", vm.uiState.value.todos[0].title)
    assertEquals("A", vm.uiState.value.todos[1].title)
    assertEquals("B", vm.uiState.value.todos[2].title)
  }

  @Test
  fun `stats repository updates persist across refreshes`() = runTest {
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
  fun `all user stats fields update correctly`() = runTest {
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
  fun fakeRepository_fetches_haveExpectedSizes() = runTest {
    val repo = com.android.sample.feature.homeScreen.FakeHomeRepository()
    val todos = repo.fetchTodos()
    val creature = repo.fetchCreatureStats()
    val userProfile = repo.fetchUserStats()

    assertEquals(3, todos.size)
    assertTrue(creature.level >= 1) // Just ensuring it returns something valid
    assertEquals("Alex", userProfile.name)
  }
}
