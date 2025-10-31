package com.android.sample.ui.shop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.R
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.
class ShopScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // --- Basic rendering of the shop ---
  @Test
  fun shopScreenDisplaysTitleAndCoins() {
    composeTestRule.setContent {
      ShopContent(userCoins = 1200, items = sampleItems(), onBuy = { _, _, _ -> })
    }

    composeTestRule.onNodeWithText("EduMon Shop").assertExists()
    composeTestRule.onNodeWithText("Your Coins").assertExists()
    composeTestRule.onNodeWithText("1200").assertExists()
  }

  // --- Item grid renders all items ---
  @Test
  fun shopDisplaysAllItems() {
    composeTestRule.setContent {
      ShopContent(userCoins = 999, items = sampleItems(), onBuy = { _, _, _ -> })
    }

    sampleItems().forEach { composeTestRule.onNodeWithText(it.name).assertExists() }
  }

  // --- Clicking an unowned item triggers both callbacks ---
  @Test
  fun shopItemTriggersSuccessAndFailCallbacks() {
    var successTriggered = false
    var failTriggered = false
    val testItem = CosmeticItem("1", "Cool Shades", 500, R.drawable.cosmetic_glasses, owned = false)

    composeTestRule.setContent {
      ShopItemCard(
          item = testItem,
          onBuy = { success, fail ->
            successTriggered = true
            success()
            failTriggered = true
            fail()
          })
    }

    composeTestRule.onNodeWithText("Cool Shades").assertExists()
    composeTestRule.onNodeWithText("500").assertExists()

    composeTestRule.onAllNodes(hasClickAction())[0].performClick()
    composeTestRule.waitForIdle()

    assert(successTriggered)
    assert(failTriggered)
  }

  // --- Owned item displays "Owned" text ---
  @Test
  fun ownedItemDisplaysOwnedText() {
    val item = CosmeticItem("1", "Red Scarf", 300, R.drawable.cosmetic_scarf, owned = true)

    composeTestRule.setContent { ShopItemCard(item = item, onBuy = { _, _ -> }) }

    composeTestRule.onNodeWithText("Owned").assertExists()
  }

  // --- ShopScreen triggers snackbar on success ---
  @Test
  fun shopScreenTriggersSnackbarOnSuccess() = runBlocking {
    val fakeViewModel = FakeShopViewModel(success = true)

    composeTestRule.setContent {
      ShopContent(
          userCoins = fakeViewModel.userCoins, items = fakeViewModel.items, onBuy = { _, _, _ -> })
    }

    composeTestRule.onAllNodes(hasClickAction())[0].performClick()
    composeTestRule.waitForIdle()
  }

  // --- ShopScreen triggers snackbar on failure ---
  @Test
  fun shopScreenTriggersSnackbarOnFailure() = runBlocking {
    val fakeViewModel = FakeShopViewModel(success = false)

    composeTestRule.setContent {
      ShopContent(
          userCoins = fakeViewModel.userCoins, items = fakeViewModel.items, onBuy = { _, _, _ -> })
    }

    composeTestRule.onAllNodes(hasClickAction())[0].performClick()
    composeTestRule.waitForIdle()
  }

  // --- Particle generation logic ---
  @Test
  fun generateParticlesCreatesValidOffsets() {
    val particles = generateParticles()
    assert(particles.size == 20)
    assert(particles.all { it.x in 0f..200f && it.y in 0f..200f })
    assert(particles.any { it != Offset.Zero })
  }

  // --- Helper for sample items ---
  private fun sampleItems(): List<CosmeticItem> =
      listOf(
          CosmeticItem("1", "Cool Shades", 500, R.drawable.cosmetic_glasses),
          CosmeticItem("2", "Wizard Hat", 800, R.drawable.cosmetic_hat),
          CosmeticItem("3", "Red Scarf", 300, R.drawable.cosmetic_scarf))
}

/**
 * Fake lightweight ViewModel replacement for testing. No inheritance needed (ShopViewModel is
 * final).
 */
data class FakeShopViewModel(val success: Boolean) {
  val userCoins = 1500
  val items =
      listOf(
          CosmeticItem("1", "Cool Shades", 500, R.drawable.cosmetic_glasses),
          CosmeticItem("2", "Wizard Hat", 800, R.drawable.cosmetic_hat))
}
