package com.android.sample.ui.shop

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShopScreenLineCoverageTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: ShopViewModel

  @Before
  fun setup() {
    // Use a real ViewModel instance (no mocking frameworks involved).
    viewModel = ViewModelProvider(composeRule.activity)[ShopViewModel::class.java]
  }

  @Test
  fun shopScreen_forcedOffline_rendersOfflineBanner() {
    composeRule.setContent { ShopScreen(viewModel = viewModel) }
    composeRule.waitForIdle()

    // Force offline so AnimatedVisibility renders OfflineBanner() and executes its Row padding
    // block.
    composeRule.runOnIdle { viewModel.setNetworkStatus(false) }

    // This text exists only inside OfflineBanner().
    composeRule.waitUntil(timeoutMillis = 3_000) {
      composeRule
          .onAllNodesWithText("You're offline — purchases are disabled")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule.onNodeWithText("You're offline — purchases are disabled").assertExists()

    // Return to online so other tests are not impacted if they share process state.
    composeRule.runOnIdle { viewModel.setNetworkStatus(true) }
  }

  @Test
  fun shopScreen_clickingBuyButtons_triggersPurchaseResultSnackbar_andClearsResult() {
    composeRule.setContent { ShopScreen(viewModel = viewModel) }
    composeRule.waitForIdle()

    // Ensure we're online so cards/buttons are clickable.
    composeRule.runOnIdle { viewModel.setNetworkStatus(true) }

    // Wait until items are available.
    composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.items.value.isNotEmpty() }

    // Try multiple purchases to increase the chance we exercise both initiated=true and
    // initiated=false
    // inside ShopScreen's onBuy lambda (e.g., eventually insufficient coins).
    repeat(4) {
      val priceToClick: Int? =
          composeRule.runOnIdle {
            // Click the most expensive currently visible (still-buyable) item first.
            // If an item becomes owned, its price button typically disappears, so we naturally
            // progress.
            viewModel.items.value.map { it.price }.distinct().maxOrNull()
          }

      if (priceToClick == null) return@repeat

      // If the price text isn't present (e.g., item owned or purchasing), skip safely.
      val priceNodes = composeRule.onAllNodesWithText(priceToClick.toString()).fetchSemanticsNodes()
      if (priceNodes.isEmpty()) return@repeat

      // This click runs ShopScreen's onBuy lambda:
      // val initiated = viewModel.buyItem(item)
      // if (initiated) triggerSuccess() else triggerFail()
      composeRule.onAllNodesWithText(priceToClick.toString()).onFirst().performClick()

      // Wait until a purchase result is produced (drives LaunchedEffect(lastPurchaseResult)).
      composeRule.waitUntil(timeoutMillis = 3_000) { viewModel.lastPurchaseResult.value != null }

      // Then wait until it is cleared again, which requires:
      // snackbarHostState.showSnackbar(message) to complete, then viewModel.clearPurchaseResult()
      composeRule.waitUntil(timeoutMillis = 12_000) { viewModel.lastPurchaseResult.value == null }
    }
  }
}
