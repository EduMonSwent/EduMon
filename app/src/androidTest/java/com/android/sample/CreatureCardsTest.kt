package com.android.sample

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.screens.CreatureHouseCard
import com.android.sample.screens.CreatureSprite
import org.junit.Rule
import org.junit.Test

@Deprecated("This test class should not be used since we removed the creature stats")
class CreatureCardsAndroidTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun creatureHouseCard_showsEnvironment_creature_and_levelChip() {
    // GIVEN
    val envRes = R.drawable.epfl_amphi_background
    val creatureRes = R.drawable.edumon
    val level = 5

    // WHEN
    rule.setContent {
      CreatureHouseCard(
          creatureResId = creatureRes,
          environmentResId = envRes,
          level = level,
      )
    }

    // THEN: environment backdrop and the creature sprite are visible
    rule.onNodeWithContentDescription("Creature environment").assertIsDisplayed()
    rule.onNodeWithContentDescription("Creature").assertIsDisplayed()

    // AND: level chip shows the correct text
    rule.onNodeWithText("Lv $level").assertIsDisplayed()
  }

  @Test
  fun creatureHouseCard_recomposes_when_level_changes() {
    var level by mutableStateOf(1)

    rule.setContent {
      CreatureHouseCard(
          creatureResId = R.drawable.edumon,
          environmentResId = R.drawable.epfl_amphi_background,
          level = level,
      )
    }

    // initial level
    rule.onNodeWithText("Lv 1").assertIsDisplayed()

    // update level on UI thread to trigger recomposition
    rule.runOnUiThread { level = 9 }
    rule.waitForIdle()

    rule.onNodeWithText("Lv 9").assertIsDisplayed()
  }

  @Test
  fun creatureSprite_animates_withoutCrashing_when_advanced() {
    // Use the sprite directly and drive the clock forward to advance animations
    rule.mainClock.autoAdvance = false

    rule.setContent { CreatureSprite(resId = R.drawable.edumon) }

    // Node exists
    rule.onNodeWithContentDescription("Creature").assertIsDisplayed()

    // Advance the animation clock a couple of cycles; this mainly ensures no crashes
    rule.mainClock.advanceTimeBy(2000L) // > 1800 tween duration
    rule.waitForIdle()
    rule.mainClock.advanceTimeBy(1600L) // > 1400 tween duration
    rule.waitForIdle()

    // Still rendered
    rule.onNodeWithContentDescription("Creature").assertIsDisplayed()
  }

  //  @Test
  //  fun creatureStatsCard_renders_all_stats_and_percentages() {
  //    val stats = CreatureStats(happiness = 72, health = 88, energy = 40)
  //
  //    rule.setContent { CreatureStatsCard(stats = stats) }
  //
  //    // Header
  //    rule.onNodeWithText("Buddy Stats").assertIsDisplayed()
  //
  //    // Labels
  //    rule.onNodeWithText("Happiness").assertIsDisplayed()
  //    rule.onNodeWithText("Health").assertIsDisplayed()
  //    rule.onNodeWithText("Energy").assertIsDisplayed()
  //
  //    // Percentages
  //    rule.onNodeWithText("72%").assertIsDisplayed()
  //    rule.onNodeWithText("88%").assertIsDisplayed()
  //    rule.onNodeWithText("40%").assertIsDisplayed()
  //  }

  @Test
  fun creatureHouseCard_click_levelChip_does_not_crash() {
    // The AssistChip has an onClick = {} (no-op). This ensures it’s interactable.
    rule.setContent {
      CreatureHouseCard(
          creatureResId = R.drawable.edumon,
          environmentResId = R.drawable.epfl_amphi_background,
          level = 3,
      )
    }

    rule.onNodeWithText("Lv 3").performClick()
    // Still visible after click
    rule.onNodeWithText("Lv 3").assertIsDisplayed()
  }
}
