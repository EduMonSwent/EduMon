package com.android.sample

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers MainActivity setContent/theme wiring. No assumptions about specific screens or auth state.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

  @get:Rule val compose = createAndroidComposeRule<MainActivity>()

  @Test
  fun launches_and_composition_exists() {
    compose.onRoot().assertExists()
  }
}
