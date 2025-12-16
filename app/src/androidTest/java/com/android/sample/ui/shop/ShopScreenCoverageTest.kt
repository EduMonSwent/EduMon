package com.android.sample.ui.shop

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
}
