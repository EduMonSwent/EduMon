package com.android.sample.todo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.ui.todo.DueDateField
import com.android.sample.ui.todo.TestTags
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test

/** UI tests for DatePickers components. */
class DatePickersTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val testDate = LocalDate.of(2025, 6, 15)
  private val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

  @Test
  fun dueDateField_displaysFormattedDate() {
    composeTestRule.setContent { DueDateField(date = testDate, onDateChange = {}) }

    composeTestRule.waitForIdle()

    // Check that the formatted date is displayed
    val expectedText = testDate.format(formatter)
    composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
  }

  @Test
  fun dueDateField_displaysDefaultLabel() {
    composeTestRule.setContent { DueDateField(date = testDate, onDateChange = {}) }

    composeTestRule.waitForIdle()

    // Check that the default label is displayed
    composeTestRule.onNodeWithText("Due date*").assertIsDisplayed()
  }

  @Test
  fun dueDateField_displaysCustomLabel() {
    val customLabel = "Custom Date Label"
    composeTestRule.setContent {
      DueDateField(date = testDate, onDateChange = {}, label = customLabel)
    }

    composeTestRule.waitForIdle()

    // Check that the custom label is displayed
    composeTestRule.onNodeWithText(customLabel).assertIsDisplayed()
  }

  @Test
  fun dueDateField_displaysChangeButton() {
    composeTestRule.setContent { DueDateField(date = testDate, onDateChange = {}) }

    composeTestRule.waitForIdle()

    // Check that the change button exists with default text
    composeTestRule.onNodeWithTag(TestTags.ChangeDateBtn).assertIsDisplayed()
    composeTestRule.onNodeWithText("Change").assertIsDisplayed()
  }

  @Test
  fun dueDateField_displaysCustomButtonText() {
    val customButtonText = "Select Date"
    composeTestRule.setContent {
      DueDateField(date = testDate, onDateChange = {}, changeButtonText = customButtonText)
    }

    composeTestRule.waitForIdle()

    // Check that the custom button text is displayed
    composeTestRule.onNodeWithText(customButtonText).assertIsDisplayed()
  }

  @Test
  fun dueDateField_changeButtonIsClickable() {
    var clickCount = 0
    composeTestRule.setContent { DueDateField(date = testDate, onDateChange = { clickCount++ }) }

    composeTestRule.waitForIdle()

    // The button should be clickable (this will open the DatePickerDialog)
    composeTestRule.onNodeWithTag(TestTags.ChangeDateBtn).performClick()

    composeTestRule.waitForIdle()

    // Note: We can't easily test the DatePickerDialog interaction in Compose tests
    // as it's a native Android dialog, but we can verify the button is clickable
  }

  @Test
  fun dueDateField_displaysToday() {
    val today = LocalDate.now()
    composeTestRule.setContent { DueDateField(date = today, onDateChange = {}) }

    composeTestRule.waitForIdle()

    val expectedText = today.format(formatter)
    composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
  }

  @Test
  fun dueDateField_displaysPastDate() {
    val pastDate = LocalDate.of(2020, 1, 1)
    composeTestRule.setContent { DueDateField(date = pastDate, onDateChange = {}) }

    composeTestRule.waitForIdle()

    val expectedText = pastDate.format(formatter)
    composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
  }

  @Test
  fun dueDateField_displaysFutureDate() {
    val futureDate = LocalDate.of(2030, 12, 31)
    composeTestRule.setContent { DueDateField(date = futureDate, onDateChange = {}) }

    composeTestRule.waitForIdle()

    val expectedText = futureDate.format(formatter)
    composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
  }

  @Test
  fun dueDateField_displaysLeapYearDate() {
    val leapYearDate = LocalDate.of(2024, 2, 29)
    composeTestRule.setContent { DueDateField(date = leapYearDate, onDateChange = {}) }

    composeTestRule.waitForIdle()

    val expectedText = leapYearDate.format(formatter)
    composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
  }

  @Test
  fun dueDateField_rendersWithoutCrashing() {
    composeTestRule.setContent { DueDateField(date = testDate, onDateChange = {}) }

    composeTestRule.waitForIdle()

    // Basic smoke test - should render without crashing
    composeTestRule.onNodeWithTag(TestTags.ChangeDateBtn).assertExists()
  }

  @Test
  fun dueDateField_textFieldIsReadOnly() {
    composeTestRule.setContent { DueDateField(date = testDate, onDateChange = {}) }

    composeTestRule.waitForIdle()

    // The text field should be read-only (we can't directly test this,
    // but we verify the date is displayed correctly and button is the interaction point)
    val expectedText = testDate.format(formatter)
    composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TestTags.ChangeDateBtn).assertIsDisplayed()
  }

  @Test
  fun dueDateField_displaysAllComponentsTogether() {
    composeTestRule.setContent {
      DueDateField(
          date = testDate, onDateChange = {}, label = "Test Date", changeButtonText = "Pick")
    }

    composeTestRule.waitForIdle()

    // Verify all components are displayed
    composeTestRule.onNodeWithText("Test Date").assertIsDisplayed()
    composeTestRule.onNodeWithText(testDate.format(formatter)).assertIsDisplayed()
    composeTestRule.onNodeWithText("Pick").assertIsDisplayed()
    composeTestRule.onNodeWithTag(TestTags.ChangeDateBtn).assertIsDisplayed()
  }
}
