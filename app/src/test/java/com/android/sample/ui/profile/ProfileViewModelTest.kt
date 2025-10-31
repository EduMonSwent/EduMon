package com.android.sample.ui.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileViewModelTest {

  private val dispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initial_state_reflects_repo_default() {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repository = repo)

    val p = vm.userProfile.value
    assertEquals("Alex", p.name)
    assertTrue(p.notificationsEnabled)
    assertTrue(p.locationEnabled)
    assertFalse(p.focusModeEnabled)
  }

  @Test
  fun toggleNotifications_flips_value() {
    val vm = ProfileViewModel(repository = FakeProfileRepository())

    val before = vm.userProfile.value.notificationsEnabled
    vm.toggleNotifications()
    val after1 = vm.userProfile.value.notificationsEnabled
    vm.toggleNotifications()
    val after2 = vm.userProfile.value.notificationsEnabled

    assertEquals(!before, after1)
    assertEquals(before, after2)
  }

  @Test
  fun toggleLocation_flips_value() {
    val vm = ProfileViewModel(repository = FakeProfileRepository())

    val before = vm.userProfile.value.locationEnabled
    vm.toggleLocation()
    val after1 = vm.userProfile.value.locationEnabled
    vm.toggleLocation()
    val after2 = vm.userProfile.value.locationEnabled

    assertEquals(!before, after1)
    assertEquals(before, after2)
  }

  @Test
  fun toggleFocusMode_flips_value() {
    val vm = ProfileViewModel(repository = FakeProfileRepository())

    val before = vm.userProfile.value.focusModeEnabled
    vm.toggleFocusMode()
    val after1 = vm.userProfile.value.focusModeEnabled
    vm.toggleFocusMode()
    val after2 = vm.userProfile.value.focusModeEnabled

    assertEquals(!before, after1)
    assertEquals(before, after2)
  }

  @Test
  fun external_repo_update_is_observed_by_viewmodel() = runTest {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repository = repo)

    repo.updateProfile(vm.userProfile.value.copy(name = "Taylor", points = 2000))

    val p = vm.userProfile.value
    assertEquals("Taylor", p.name)
    assertEquals(2000, p.points)
  }

  // --- Reward system tests ---------------------------------------------------

  @Test
  fun addCoins_withPositiveAmount_increasesUserCoins() = runTest {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repository = repo)

    val before = vm.userProfile.value.coins
    vm.addCoins(100)
    val after = vm.userProfile.value.coins

    assertEquals(before + 100, after)
  }

  @Test
  fun addCoins_withZeroOrNegativeAmount_doesNothing() = runTest {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repository = repo)

    val before = vm.userProfile.value.coins
    vm.addCoins(0) // should be ignored
    vm.addCoins(-50) // should also be ignored
    val after = vm.userProfile.value.coins

    assertEquals(before, after)
  }
}
