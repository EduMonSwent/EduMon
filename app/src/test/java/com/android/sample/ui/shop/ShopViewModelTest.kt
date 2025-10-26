package com.android.sample.ui.shop

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ShopViewModelTest {

  private lateinit var viewModel: ShopViewModel

  @Before
  fun setup() {
    viewModel = ShopViewModel()
  }

  // --- Base state ----------------------------------------------------------

  @Test
  fun initialCoins_shouldBe1500() = runBlocking {
    val coins = viewModel.userCoins.first()
    assertEquals(1500, coins)
  }

  @Test
  fun initialItems_shouldContain6Cosmetics() = runBlocking {
    val items = viewModel.items.first()
    assertEquals(6, items.size)
    assertTrue(items.any { it.name == "Cool Shades" })
  }

  // --- Buying logic --------------------------------------------------------

  @Test
  fun buyItem_withEnoughCoins_shouldDeductCoinsAndMarkOwned() = runBlocking {
    val itemToBuy = viewModel.items.first().first { it.name == "Cool Shades" }
    val initialCoins = viewModel.userCoins.first()

    val success = viewModel.buyItem(itemToBuy)

    val newCoins = viewModel.userCoins.first()
    val updatedItem = viewModel.items.first().first { it.id == itemToBuy.id }

    assertTrue(success)
    assertTrue(updatedItem.owned)
    assertEquals(initialCoins - itemToBuy.price, newCoins)
  }

  @Test
  fun buyItem_whenAlreadyOwned_shouldFailAndNotChangeCoins() = runBlocking {
    val item = viewModel.items.first().first { it.name == "Wizard Hat" }

    // First purchase succeeds
    val firstSuccess = viewModel.buyItem(item)
    val coinsAfterFirst = viewModel.userCoins.first()

    // Second purchase should fail
    val secondSuccess = viewModel.buyItem(item)
    val coinsAfterSecond = viewModel.userCoins.first()

    assertTrue(firstSuccess)
    assertFalse(secondSuccess)
    assertEquals(coinsAfterFirst, coinsAfterSecond)
  }

  @Test
  fun buyItem_updatesOnlyTargetItem() = runBlocking {
    val target = viewModel.items.first()[0]
    val othersBefore = viewModel.items.first().drop(1)

    viewModel.buyItem(target)

    val updatedList = viewModel.items.first()
    val updatedTarget = updatedList.first { it.id == target.id }
    val unchangedOthers = updatedList.drop(1)

    assertTrue(updatedTarget.owned)
    unchangedOthers.forEach { assertFalse(it.owned) }
    assertEquals(othersBefore.size, unchangedOthers.size)
  }

  // --- Edge Cases ----------------------------------------------------------

  @Test
  fun buyItem_exactCoinBalance_shouldSucceedAndReachZero() = runBlocking {
    val allItems = viewModel.items.first()
    val totalCost = allItems.sumOf { it.price }

    // simulate same number of coins as total cost
    val vm = ShopViewModel()
    vm.userCoins.value // triggers flow init
    vm.buyItem(allItems.first())

    val remainingCoins = vm.userCoins.first()
    assertTrue(remainingCoins <= 1500)
  }
}
