package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.R
import com.android.sample.ui.planner.AIRecommendationCard
import org.junit.Rule
import org.junit.Test

class AIRecommendationCardTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun showsAIRecommendationText() {
    composeTestRule.setContent {
      AIRecommendationCard(
          recommendationText = stringResource(R.string.ai_recommendation_calculus_example))
    }

    // Check for part of the recommendation text (substring for robustness)
    composeTestRule.onNodeWithText("Calculus", substring = true).assertExists()
  }
}
