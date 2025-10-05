package com.android.sample.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun allSectionsAreDisplayed() {
        composeTestRule.setContent {
            ProfileScreen()
        }


        composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
            .performScrollToNode(hasTestTag(ProfileScreenTestTags.PET_SECTION))
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.PET_SECTION).assertIsDisplayed()

        composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
            .performScrollToNode(hasTestTag(ProfileScreenTestTags.PROFILE_CARD))
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_CARD).assertIsDisplayed()

        composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
            .performScrollToNode(hasTestTag(ProfileScreenTestTags.STATS_CARD))
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.STATS_CARD).assertIsDisplayed()

        composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
            .performScrollToNode(hasTestTag(ProfileScreenTestTags.CUSTOMIZE_PET_SECTION))
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.CUSTOMIZE_PET_SECTION).assertIsDisplayed()

        composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
            .performScrollToNode(hasTestTag(ProfileScreenTestTags.SETTINGS_CARD))
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_CARD).assertIsDisplayed()

        composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
            .performScrollToNode(hasTestTag(ProfileScreenTestTags.ACCOUNT_ACTIONS_SECTION))
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.ACCOUNT_ACTIONS_SECTION).assertIsDisplayed()
    }
}
