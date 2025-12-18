package com.android.sample

// This code has been written partially using A.I (LLM).

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.flashcards.util.ConnectivityDeps
import com.android.sample.ui.flashcards.util.ConnectivityObserver
import com.android.sample.ui.flashcards.util.DisposableHandle
import com.android.sample.ui.flashcards.util.OnlineChecker
import com.android.sample.ui.profile.ProfileScreenTestTags
import com.android.sample.ui.session.StudySessionTestTags
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppFullFlowOfflineStudyProfileE2ETest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakeConnectivity: FakeConnectivity
  private var savedOnlineChecker: OnlineChecker? = null
  private var savedObserver: ConnectivityObserver? = null

  private companion object {
    private const val TIMEOUT_MS = 5_000L

    private const val IMPORT_DECK_FAB_TAG = "ImportDeckFab"
    private const val IMPORT_ROOT_TAG = "ImportDeckScreenRoot"
    private const val CREATE_DECK_BUTTON_TAG = "CreateDeckButton"

    private const val IMPORT_BUTTON_TEXT = "Import"
    private const val SHARE_CODE_LABEL_TEXT = "Share Code"
    private const val BACK_CONTENT_DESC = "Back"

    private const val TOKEN_VALUE = "E2E_TOKEN_123"
    private const val SUBJECT_NAME = "E2E Subject"

    private const val MAX_BACK_PRESSES = 10

    // Title used by ScreenWithTopBar for the focus screen
    private const val FOCUS_MODE_TITLE = "Focus Mode"
  }

  @Before
  fun setup() {
    AppRepositories = FakeRepositoriesProvider

    savedOnlineChecker = ConnectivityDeps.onlineChecker
    savedObserver = ConnectivityDeps.observer

    fakeConnectivity = FakeConnectivity(initialOnline = false)
    ConnectivityDeps.onlineChecker = OnlineChecker { _ -> fakeConnectivity.isOnlineNow }
    ConnectivityDeps.observer = fakeConnectivity

    composeRule.setContent {
      EduMonNavHost(startDestination = AppDestination.Home.route, onSignOut = {})
    }
  }

  @After
  fun tearDown() {
    savedOnlineChecker?.let { ConnectivityDeps.onlineChecker = it }
    savedObserver?.let { ConnectivityDeps.observer = it }
  }

  @Test
  fun offlineImport_thenOnlineRecovery_thenStudy_thenProfileFocusMode() {
    // Home -> Flashcards
    openDrawerFromHome()
    clickDrawerDestination(AppDestination.Flashcards.route)

    // Flashcards list sanity check
    composeRule.onNodeWithTag(CREATE_DECK_BUTTON_TAG).assertIsDisplayed()

    // Open Import screen
    composeRule.onNodeWithTag(IMPORT_DECK_FAB_TAG).performClick()
    composeRule.onNodeWithTag(IMPORT_ROOT_TAG).assertIsDisplayed()

    val offlineMsg = composeRule.activity.getString(R.string.flashcards_offline_import_deck)

    // Offline assertions
    composeRule.onNodeWithText(offlineMsg).assertIsDisplayed()
    composeRule.onNodeWithText(IMPORT_BUTTON_TEXT).assertIsNotEnabled()

    // Go online
    fakeConnectivity.emitOnline(true)

    // Offline message disappears
    composeRule.waitUntil(TIMEOUT_MS) {
      runCatching {
            composeRule.onNodeWithText(offlineMsg).assertDoesNotExist()
            true
          }
          .getOrDefault(false)
    }

    // Enter token and start import (button should enable then disable after click)
    composeRule.onNodeWithText(SHARE_CODE_LABEL_TEXT).performClick()
    composeRule.onNodeWithText(SHARE_CODE_LABEL_TEXT).performTextInput(TOKEN_VALUE)
    composeRule.onNodeWithText(IMPORT_BUTTON_TEXT).assertIsEnabled()
    composeRule.onNodeWithText(IMPORT_BUTTON_TEXT).performClick()

    composeRule.waitUntil(TIMEOUT_MS) {
      runCatching {
            composeRule.onNodeWithText(IMPORT_BUTTON_TEXT).assertIsNotEnabled()
            true
          }
          .getOrDefault(false)
    }

    // Back to deck list (Import screen uses back icon with contentDescription = "Back")
    composeRule.onNodeWithContentDescription(BACK_CONTENT_DESC).performClick()
    composeRule.onNodeWithTag(CREATE_DECK_BUTTON_TAG).assertIsDisplayed()

    // Back to Home
    goBackToHome()

    // Home -> Study, create a subject (real state change)
    openDrawerFromHome()
    clickDrawerDestination(AppDestination.Study.route)
    composeRule.onNodeWithTag(StudySessionTestTags.TITLE).assertIsDisplayed()

    val subjectPlaceholder =
        composeRule.activity.getString(R.string.study_session_new_subject_placeholder)
    val addSubjectText = composeRule.activity.getString(R.string.study_session_add_subject_button)

    composeRule.onNodeWithText(subjectPlaceholder).performClick()
    composeRule.onNodeWithText(subjectPlaceholder).performTextInput(SUBJECT_NAME)
    composeRule.onNodeWithText(addSubjectText).performClick()
    composeRule.onNodeWithText(SUBJECT_NAME).assertIsDisplayed()

    // Back to Home
    goBackToHome()

    // Home -> Profile
    openDrawerFromHome()
    clickDrawerDestination(AppDestination.Profile.route)
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN).assertIsDisplayed()

    // LazyColumn: scroll until the switches are composed, then click
    scrollProfileTo(ProfileScreenTestTags.SWITCH_LOCATION)
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.SWITCH_LOCATION, useUnmergedTree = true)
        .performClick()

    scrollProfileTo(ProfileScreenTestTags.SWITCH_FOCUS_MODE)
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE, useUnmergedTree = true)
        .performClick()

    // CI-compliant: assert via a UNIQUE node (top bar title tag), avoid text collisions
    composeRule.waitUntil(TIMEOUT_MS) {
      runCatching {
            composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
            composeRule
                .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
                .assertTextEquals(FOCUS_MODE_TITLE)
            true
          }
          .getOrDefault(false)
    }

    // Back to Profile, then Home (flow completes)
    composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN).assertIsDisplayed()
    goBackToHome()
  }

  private fun openDrawerFromHome() {
    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
  }

  private fun clickDrawerDestination(route: String) {
    composeRule.onNodeWithTag(HomeTestTags.drawerTag(route)).performClick()
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
  }

  private fun scrollProfileTo(tag: String) {
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(tag))
  }

  private fun goBackToHome() {
    repeat(MAX_BACK_PRESSES) {
      val homeMenuExists =
          composeRule.onAllNodesWithTag(HomeTestTags.MENU_BUTTON).fetchSemanticsNodes().isNotEmpty()
      if (homeMenuExists) return

      val backExists =
          composeRule
              .onAllNodesWithTag(NavigationTestTags.GO_BACK_BUTTON)
              .fetchSemanticsNodes()
              .isNotEmpty()
      if (!backExists) return

      composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    }
    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertIsDisplayed()
  }
}

private class FakeConnectivity(initialOnline: Boolean) : ConnectivityObserver {

  @Volatile
  var isOnlineNow: Boolean = initialOnline
    private set

  @Volatile private var callback: ((Boolean) -> Unit)? = null

  override fun observe(context: Context, onOnlineChanged: (Boolean) -> Unit): DisposableHandle {
    callback = onOnlineChanged
    onOnlineChanged(isOnlineNow)
    return DisposableHandle { callback = null }
  }

  fun emitOnline(isOnline: Boolean) {
    isOnlineNow = isOnline
    callback?.invoke(isOnline)
  }
}
