package com.android.sample.screen.calendar

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import com.android.sample.ui.calendar.CalendarHeader
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarHeaderTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun header_shows_title_and_prev_next_callbacks() {
    val ctx = composeRule.activity
    var prevClicks = 0
    var nextClicks = 0

    composeRule.setContent {
      MaterialTheme {
        CalendarHeader(
            title = "My Calendar", onPrevClick = { prevClicks++ }, onNextClick = { nextClicks++ })
      }
    }

    // Title is displayed
    composeRule.onNodeWithText("My Calendar").assertExists()

    // Buttons with localized content descriptions
    val prevCd = ctx.getString(R.string.previous)
    val nextCd = ctx.getString(R.string.next)

    composeRule.onNodeWithContentDescription(prevCd).performClick()
    composeRule.onNodeWithContentDescription(nextCd).performClick()
    composeRule.onNodeWithContentDescription(nextCd).performClick()

    assertEquals(1, prevClicks)
    assertEquals(2, nextClicks)
  }
}
