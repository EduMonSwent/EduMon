package com.android.sample

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.screen.MainScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented test for MainActivity rendering WeekProgDailyObj. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest : TestCase() {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun mainRendersWeekProgressUi_andExpandsWeeks() = run {
    step("Launch MainActivity and verify core sections") {
      ComposeScreen.onComposeScreen<MainScreen>(composeTestRule) {
        weekRootCard { assertIsDisplayed() }
        weekProgressBar { assertIsDisplayed() }
        objectivesSection { assertIsDisplayed() }
        weekDotsRow { assertIsDisplayed() }
      }
    }

    step("Expand week list and verify it becomes visible") {
      ComposeScreen.onComposeScreen<MainScreen>(composeTestRule) {
        weekProgressToggle {
          assertIsDisplayed()
          performClick()
        }
        weeksList { assertIsDisplayed() }
      }
    }
  }
}
