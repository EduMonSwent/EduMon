package com.android.sample.ui.shop

import com.android.sample.data.FakeUserStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {

  private lateinit var viewModel: ShopViewModel
  private lateinit var userStatsRepo: FakeUserStatsRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    userStatsRepo = FakeUserStatsRepository()
    // Inject the fake repository to avoid using the real AppRepositories singleton
    viewModel = ShopViewModel(userStatsRepository = userStatsRepo)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    testDispatcher.cancel()
  }

  // --- Base state ----------------------------------------------------------

  @Test
  fun initialCoins_shouldBe1500() = runTest {
    val repoWithCoins = FakeUserStatsRepository()
    repoWithCoins.updateCoins(1500)
    val vm = ShopViewModel(repoWithCoins)

    // Start a background collector to ensure stateIn is active and updates value
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.userCoins.collect() }

    // Wait for the correct value to be emitted
    val coins = vm.userCoins.first { it == 1500 }
    assertEquals(1500, coins)
  }

  @Test
  fun initialItems_shouldContain6Cosmetics() = runTest {
    // Ensure items flow is collected if needed (StateFlow with initial value usually ok)
    val items = viewModel.items.value
    assertEquals(6, items.size)
    assertTrue(items.any { it.name == "Cool Shades" })
  }

  // --- Buying logic --------------------------------------------------------

  @Test
  fun buyItem_withEnoughCoins_shouldDeductCoinsAndMarkOwned() = runTest {
    userStatsRepo.updateCoins(2000)
    val vm = ShopViewModel(userStatsRepo)

    // Keep userCoins active
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.userCoins.collect() }

    // Wait for sync
    vm.userCoins.first { it == 2000 }

    val itemToBuy = vm.items.value.first { it.name == "Cool Shades" } // 500 cost

    val success = vm.buyItem(itemToBuy)

    assertTrue("Buying should succeed", success)

    val newCoins = userStatsRepo.stats.value.coins
    assertEquals(2000 - itemToBuy.price, newCoins)

    // Use first() here to wait for flow emission instead of accessing .value which might be stale
    val updatedItems = vm.items.first { list -> list.find { it.id == itemToBuy.id }?.owned == true }
    val updatedItem = updatedItems.first { it.id == itemToBuy.id }
    assertTrue(updatedItem.owned)
  }

  @Test
  fun buyItem_whenAlreadyOwned_shouldFailAndNotChangeCoins() = runTest {
    userStatsRepo.updateCoins(2000)
    val vm = ShopViewModel(userStatsRepo)

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.userCoins.collect() }
    vm.userCoins.first { it == 2000 }

    val item = vm.items.value.first { it.name == "Wizard Hat" }

    // First purchase
    val firstSuccess = vm.buyItem(item)
    assertTrue(firstSuccess)

    // Verify coins update after first purchase
    val expectedCoins = 2000 - item.price
    vm.userCoins.first { it == expectedCoins }

    // Wait for item ownership update
    vm.items.first { list -> list.any { it.id == item.id && it.owned } }

    // Fetch updated item from VM which should be marked as owned
    val updatedItem = vm.items.value.first { it.id == item.id }

    // Second purchase with updated item
    val secondSuccess = vm.buyItem(updatedItem)
    assertFalse("Should fail if already owned", secondSuccess)

    assertEquals(expectedCoins, userStatsRepo.stats.value.coins)
  }

  @Test
  fun buyItem_updatesOnlyTargetItem() = runTest {
    userStatsRepo.updateCoins(5000)
    val vm = ShopViewModel(userStatsRepo)

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.userCoins.collect() }
    vm.userCoins.first { it == 5000 }

    val target = vm.items.value[0]
    val othersBefore = vm.items.value.drop(1)

    vm.buyItem(target)

    // Wait for update
    val updatedList = vm.items.first { list -> list.any { it.id == target.id && it.owned } }

    val updatedTarget = updatedList.first { it.id == target.id }
    val unchangedOthers = updatedList.drop(1)

    assertTrue(updatedTarget.owned)
    unchangedOthers.forEach { assertFalse(it.owned) }
    assertEquals(othersBefore.size, unchangedOthers.size)
  }

  // --- Edge Cases ----------------------------------------------------------

  @Test
  fun buyItem_exactCoinBalance_shouldSucceedAndReachZero() = runTest {
    val initialRepo = FakeUserStatsRepository()
    val specificVm = ShopViewModel(initialRepo)

    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      specificVm.userCoins.collect()
    }

    val item = specificVm.items.value.first()
    initialRepo.updateCoins(item.price)

    specificVm.userCoins.first { it == item.price }

    val success = specificVm.buyItem(item)

    assertTrue("Should succeed with exact balance", success)
    assertEquals(0, initialRepo.stats.value.coins)
  }
}
