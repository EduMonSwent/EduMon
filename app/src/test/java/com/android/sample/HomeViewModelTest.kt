package com.android.sample

import com.android.sample.repositories.FakeHomeRepository
import com.android.sample.repositories.HomeRepository
import com.android.sample.repositories.HomeUiState
import com.android.sample.repositories.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

  private class TestRepository(
      private val todos: List<Todo> =
          listOf(
              Todo("1", "A", "Today 10:00", true),
              Todo("2", "B", "Today 12:00"),
              Todo("3", "C", "Tomorrow")),
      private val creature: CreatureStats =
          CreatureStats(happiness = 10, health = 20, energy = 30, level = 7),
      private val user: UserStats =
          UserStats(streakDays = 3, points = 99, studyTodayMin = 15, dailyGoalMin = 120),
      private val quote: String = "Test quote"
  ) : HomeRepository {
    override suspend fun fetchTodos(): List<Todo> = todos

    override suspend fun fetchCreatureStats(): CreatureStats = creature

    override suspend fun fetchUserStats(): UserStats = user

    override fun dailyQuote(nowMillis: Long): String = quote
  }

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial refresh populates state and stops loading`() = runTest {
    val vm = HomeViewModel(repository = TestRepository())
    // init{} triggers refresh immediately
    assertTrue(vm.uiState.value.isLoading)
    advanceUntilIdle()

    val s = vm.uiState.value
    assertFalse(s.isLoading)
    assertEquals(3, s.todos.size)
    assertEquals("A", s.todos.first().title)
    assertEquals(7, s.creatureStats.level)
    assertEquals(99, s.userStats.points)
    assertEquals("Test quote", s.quote)
  }

  @Test
  fun `refresh toggles loading then updates values`() = runTest {
    val vm = HomeViewModel(repository = TestRepository(quote = "Q1"))
    advanceUntilIdle()
    assertEquals("Q1", vm.uiState.value.quote)

    // call refresh again and check the loading flip
    vm.refresh()
    // Immediately after call, still on main thread
    assertTrue(vm.uiState.value.isLoading)
    advanceUntilIdle()
    assertFalse(vm.uiState.value.isLoading)
  }

  @Test
  fun `supports empty todos`() = runTest {
    val vm = HomeViewModel(repository = TestRepository(todos = emptyList(), quote = "EmptyQ"))
    advanceUntilIdle()

    val s = vm.uiState.value
    assertTrue(s.todos.isEmpty())
    assertEquals("EmptyQ", s.quote)
  }

  @Test
  fun `different repos produce different quotes`() = runTest {
    val vm1 = HomeViewModel(repository = TestRepository(quote = "Q1"))
    advanceUntilIdle()
    assertEquals("Q1", vm1.uiState.value.quote)

    val vm2 = HomeViewModel(repository = TestRepository(quote = "Q2"))
    advanceUntilIdle()
    assertEquals("Q2", vm2.uiState.value.quote)
  }

  @Test
  fun fakeRepository_dailyQuote_changesAcrossDays_andWraps() {
    val repo = FakeHomeRepository()
    val d0 = repo.dailyQuote(nowMillis = 0L)
    val d1 = repo.dailyQuote(nowMillis = 86_400_000L) // +1 day
    val d6 = repo.dailyQuote(nowMillis = 6L * 86_400_000L) // +6 days (wraps mod 5)

    // Same day -> same value; different day -> typically different; day 6 == day 1 (wrap by 5)
    assertEquals(d1, d6)
    assertNotEquals(d0, d1)
  }

  @Test
  fun fakeRepository_fetches_haveExpectedSizes() = runTest {
    val repo = FakeHomeRepository()
    val todos = repo.fetchTodos()
    val creature = repo.fetchCreatureStats()
    val user = repo.fetchUserStats()

    assertEquals(3, todos.size)
    assertTrue(creature.level >= 1)
    assertTrue(user.dailyGoalMin > 0)
  }

  @Test
  fun homeUiState_defaults_areSane() {
    val s = HomeUiState()
    assertTrue(s.isLoading)
    assertTrue(s.todos.isEmpty())
    assertEquals(5, s.creatureStats.level)
    assertEquals(180, s.userStats.dailyGoalMin)
    assertEquals("", s.quote)
  }
}
