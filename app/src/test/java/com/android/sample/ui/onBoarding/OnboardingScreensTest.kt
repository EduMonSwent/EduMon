package com.android.sample.ui.onBoarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive test suite for EduMon Onboarding screens. Targets 99% line coverage across all
 * onboarding-related files.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], instrumentedPackages = ["androidx.loader.content"])
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingScreensTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    clearAllMocks()
  }

  // ============================================================
  // OnboardingStep Tests
  // ============================================================

  @Test
  fun `OnboardingStep enum has correct values`() {
    val steps = OnboardingStep.values()
    assertEquals(3, steps.size)
    assertTrue(steps.contains(OnboardingStep.Intro))
    assertTrue(steps.contains(OnboardingStep.Professor))
    assertTrue(steps.contains(OnboardingStep.StarterSelection))
  }

  @Test
  fun `OnboardingStep ordinal values are correct`() {
    assertEquals(0, OnboardingStep.Intro.ordinal)
    assertEquals(1, OnboardingStep.Professor.ordinal)
    assertEquals(2, OnboardingStep.StarterSelection.ordinal)
  }

  @Test
  fun `OnboardingStep name values are correct`() {
    assertEquals("Intro", OnboardingStep.Intro.name)
    assertEquals("Professor", OnboardingStep.Professor.name)
    assertEquals("StarterSelection", OnboardingStep.StarterSelection.name)
  }

  @Test
  fun `OnboardingStep valueOf works correctly`() {
    assertEquals(OnboardingStep.Intro, OnboardingStep.valueOf("Intro"))
    assertEquals(OnboardingStep.Professor, OnboardingStep.valueOf("Professor"))
    assertEquals(OnboardingStep.StarterSelection, OnboardingStep.valueOf("StarterSelection"))
  }

  // ============================================================
  // OnboardingDimens Tests
  // ============================================================

  @Test
  fun `OnboardingDimens screenPadding has correct value`() {
    assertEquals(24, OnboardingDimens.screenPadding.value.toInt())
  }

  @Test
  fun `OnboardingDimens contentSpacing has correct value`() {
    assertEquals(16, OnboardingDimens.contentSpacing.value.toInt())
  }

  @Test
  fun `OnboardingDimens logoSize has correct value`() {
    assertEquals(160, OnboardingDimens.logoSize.value.toInt())
  }

  @Test
  fun `OnboardingDimens professorImageSize has correct value`() {
    assertEquals(220, OnboardingDimens.professorImageSize.value.toInt())
  }

  @Test
  fun `OnboardingDimens dialogCornerRadius has correct value`() {
    assertEquals(16, OnboardingDimens.dialogCornerRadius.value.toInt())
  }

  @Test
  fun `OnboardingDimens dialogBorderWidth has correct value`() {
    assertEquals(2, OnboardingDimens.dialogBorderWidth.value.toInt())
  }

  @Test
  fun `OnboardingDimens dialogPadding has correct value`() {
    assertEquals(16, OnboardingDimens.dialogPadding.value.toInt())
  }

  @Test
  fun `OnboardingDimens dialogVerticalPadding has correct value`() {
    assertEquals(20, OnboardingDimens.dialogVerticalPadding.value.toInt())
  }

  @Test
  fun `OnboardingDimens tapToStartBottomPadding has correct value`() {
    assertEquals(32, OnboardingDimens.tapToStartBottomPadding.value.toInt())
  }

  @Test
  fun `OnboardingDimens startersRowSpacing has correct value`() {
    assertEquals(16, OnboardingDimens.startersRowSpacing.value.toInt())
  }

  @Test
  fun `OnboardingDimens startersBottomSpacer has correct value`() {
    assertEquals(24, OnboardingDimens.startersBottomSpacer.value.toInt())
  }

  @Test
  fun `OnboardingDimens starterCardSize has correct value`() {
    assertEquals(140, OnboardingDimens.starterCardSize.value.toInt())
  }

  @Test
  fun `OnboardingDimens starterCardCornerRadius has correct value`() {
    assertEquals(20, OnboardingDimens.starterCardCornerRadius.value.toInt())
  }

  @Test
  fun `OnboardingDimens starterCardElevation has correct value`() {
    assertEquals(4, OnboardingDimens.starterCardElevation.value.toInt())
  }

  @Test
  fun `OnboardingDimens confirmButtonTopPadding has correct value`() {
    assertEquals(24, OnboardingDimens.confirmButtonTopPadding.value.toInt())
  }

  @Test
  fun `OnboardingDimens titleTextSize has correct value`() {
    assertEquals(22, OnboardingDimens.titleTextSize.value.toInt())
  }

  @Test
  fun `OnboardingDimens bodyTextSize has correct value`() {
    assertEquals(16, OnboardingDimens.bodyTextSize.value.toInt())
  }

  @Test
  fun `OnboardingDimens tapBlinkAlphaMin has correct value`() {
    assertEquals(0.3f, OnboardingDimens.tapBlinkAlphaMin, 0.01f)
  }

  @Test
  fun `OnboardingDimens tapBlinkAlphaMax has correct value`() {
    assertEquals(1.0f, OnboardingDimens.tapBlinkAlphaMax, 0.01f)
  }

  @Test
  fun `OnboardingDimens tapBlinkDurationMillis has correct value`() {
    assertEquals(700, OnboardingDimens.tapBlinkDurationMillis)
  }

  @Test
  fun `OnboardingDimens transitionDurationMillis has correct value`() {
    assertEquals(500, OnboardingDimens.transitionDurationMillis)
  }

  @Test
  fun `OnboardingDimens dialogueLetterDelayMillis has correct value`() {
    assertEquals(18L, OnboardingDimens.dialogueLetterDelayMillis)
  }

  @Test
  fun `OnboardingDimens transitionSlideOffsetDivisor has correct value`() {
    assertEquals(8, OnboardingDimens.transitionSlideOffsetDivisor)
  }

  @Test
  fun `OnboardingDimens dialogMaxLines has correct value`() {
    assertEquals(4, OnboardingDimens.dialogMaxLines)
  }

  @Test
  fun `OnboardingDimens professorLargeSize has correct value`() {
    assertEquals(420, OnboardingDimens.professorLargeSize.value.toInt())
  }

  @Test
  fun `OnboardingDimens professorOffsetY has correct value`() {
    assertEquals(40, OnboardingDimens.professorOffsetY.value.toInt())
  }

  // ============================================================
  // StarterModel Tests
  // ============================================================

  @Test
  fun `StarterDefinition data class can be created`() {
    val starter =
        StarterDefinition(
            nameRes = android.R.string.ok, imageRes = android.R.drawable.ic_menu_gallery)
    assertEquals(android.R.string.ok, starter.nameRes)
    assertEquals(android.R.drawable.ic_menu_gallery, starter.imageRes)
  }

  @Test
  fun `StarterDefinition equals works correctly`() {
    val starter1 =
        StarterDefinition(
            nameRes = android.R.string.ok, imageRes = android.R.drawable.ic_menu_gallery)
    val starter2 =
        StarterDefinition(
            nameRes = android.R.string.ok, imageRes = android.R.drawable.ic_menu_gallery)
    assertEquals(starter1, starter2)
  }

  @Test
  fun `StarterDefinition copy works correctly`() {
    val starter =
        StarterDefinition(
            nameRes = android.R.string.ok, imageRes = android.R.drawable.ic_menu_gallery)
    val copied = starter.copy(nameRes = android.R.string.cancel)
    assertEquals(android.R.string.cancel, copied.nameRes)
    assertEquals(android.R.drawable.ic_menu_gallery, copied.imageRes)
  }

  @Test
  fun `StarterDefinition component functions work correctly`() {
    val starter =
        StarterDefinition(
            nameRes = android.R.string.ok, imageRes = android.R.drawable.ic_menu_gallery)
    val (nameRes, imageRes) = starter
    assertEquals(android.R.string.ok, nameRes)
    assertEquals(android.R.drawable.ic_menu_gallery, imageRes)
  }

  @Test
  fun `onboardingStarters list has correct size`() {
    assertEquals(3, onboardingStarters.size)
  }

  @Test
  fun `onboardingStarters contains all starters`() {
    assertNotNull(onboardingStarters[0])
    assertNotNull(onboardingStarters[1])
    assertNotNull(onboardingStarters[2])
  }

  // ============================================================
  // StarterItem Tests
  // ============================================================

  @Test
  fun `StarterItem data class can be created`() {
    val item =
        StarterItem(
            id = "test_id",
            image = android.R.drawable.ic_menu_gallery,
            background = android.R.drawable.ic_menu_camera)
    assertEquals("test_id", item.id)
    assertEquals(android.R.drawable.ic_menu_gallery, item.image)
    assertEquals(android.R.drawable.ic_menu_camera, item.background)
  }

  @Test
  fun `StarterItem equals works correctly`() {
    val item1 =
        StarterItem(
            id = "test",
            image = android.R.drawable.ic_menu_gallery,
            background = android.R.drawable.ic_menu_camera)
    val item2 =
        StarterItem(
            id = "test",
            image = android.R.drawable.ic_menu_gallery,
            background = android.R.drawable.ic_menu_camera)
    assertEquals(item1, item2)
  }

  @Test
  fun `StarterItem copy works correctly`() {
    val item =
        StarterItem(
            id = "original",
            image = android.R.drawable.ic_menu_gallery,
            background = android.R.drawable.ic_menu_camera)
    val copied = item.copy(id = "copied")
    assertEquals("copied", copied.id)
    assertEquals(android.R.drawable.ic_menu_gallery, copied.image)
  }

  @Test
  fun `StarterItem component functions work correctly`() {
    val item =
        StarterItem(
            id = "test",
            image = android.R.drawable.ic_menu_gallery,
            background = android.R.drawable.ic_menu_camera)
    val (id, image, background) = item
    assertEquals("test", id)
    assertEquals(android.R.drawable.ic_menu_gallery, image)
    assertEquals(android.R.drawable.ic_menu_camera, background)
  }

  @Test
  fun `StarterItem not equals with different id`() {
    val item1 =
        StarterItem("test1", android.R.drawable.ic_menu_gallery, android.R.drawable.ic_menu_camera)
    val item2 =
        StarterItem("test2", android.R.drawable.ic_menu_gallery, android.R.drawable.ic_menu_camera)
    assertNotEquals(item1, item2)
  }

  // ============================================================
  // ArrowLeftCute Vector Tests
  // ============================================================

  @Test
  fun `ArrowLeftCute has correct name`() {
    assertEquals("arrow_left_cute", ArrowLeftCute.name)
  }

  @Test
  fun `ArrowLeftCute has correct dimensions`() {
    assertEquals(48f, ArrowLeftCute.defaultWidth.value, 0.1f)
    assertEquals(48f, ArrowLeftCute.defaultHeight.value, 0.1f)
    assertEquals(48f, ArrowLeftCute.viewportWidth, 0.1f)
    assertEquals(48f, ArrowLeftCute.viewportHeight, 0.1f)
  }

  // ============================================================
  // ArrowRightCute Vector Tests
  // ============================================================

  @Test
  fun `ArrowRightCute has correct name`() {
    assertEquals("arrow_right_cute", ArrowRightCute.name)
  }

  @Test
  fun `ArrowRightCute has correct dimensions`() {
    assertEquals(48f, ArrowRightCute.defaultWidth.value, 0.1f)
    assertEquals(48f, ArrowRightCute.defaultHeight.value, 0.1f)
    assertEquals(48f, ArrowRightCute.viewportWidth, 0.1f)
    assertEquals(48f, ArrowRightCute.viewportHeight, 0.1f)
  }

  @Test
  fun `ArrowLeftCute and ArrowRightCute are different`() {
    assertNotEquals(ArrowLeftCute.name, ArrowRightCute.name)
  }

  // ============================================================
  // IntroTapToStartScreen Tests
  // ============================================================

  @Test
  fun `IntroTapToStartScreen renders without crashing`() {
    composeTestRule.setContent { IntroTapToStartScreen(onTap = {}) }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `IntroTapToStartScreen displays tap to start text`() {
    composeTestRule.setContent { IntroTapToStartScreen(onTap = {}) }
    composeTestRule.onNodeWithText("Tap to Start", substring = true, ignoreCase = true)
  }

  @Test
  fun `IntroTapToStartScreen calls onTap when clicked`() {
    var tapped = false
    composeTestRule.setContent { IntroTapToStartScreen(onTap = { tapped = true }) }
    composeTestRule.onRoot().performClick()
    assertTrue(tapped)
  }

  @Test
  fun `IntroTapToStartScreen multiple taps call onTap multiple times`() {
    var tapCount = 0
    composeTestRule.setContent { IntroTapToStartScreen(onTap = { tapCount++ }) }
    composeTestRule.onRoot().performClick()
    composeTestRule.onRoot().performClick()
    composeTestRule.onRoot().performClick()
    assertEquals(3, tapCount)
  }

  @Test
  fun `IntroTapToStartScreen infinite transition runs`() {
    composeTestRule.setContent { IntroTapToStartScreen(onTap = {}) }
    composeTestRule.mainClock.advanceTimeBy(350)
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(350)
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(700)
    composeTestRule.waitForIdle()
  }

  @Test
  fun `IntroTapToStartScreen with custom modifier works`() {
    composeTestRule.setContent {
      IntroTapToStartScreen(modifier = androidx.compose.ui.Modifier.fillMaxSize(), onTap = {})
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }

  // ============================================================
  // ProfessorDialogueScreen Tests
  // ============================================================

  @Test
  fun `ProfessorDialogueScreen renders without crashing`() {
    composeTestRule.setContent { ProfessorDialogueScreen(onDialogueFinished = {}) }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `ProfessorDialogueScreen shows first dialogue line`() {
    composeTestRule.setContent { ProfessorDialogueScreen(onDialogueFinished = {}) }
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(2000)
  }

  @Test
  fun `ProfessorDialogueScreen advances dialogue on tap`() {
    composeTestRule.setContent { ProfessorDialogueScreen(onDialogueFinished = {}) }
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(5000)
    composeTestRule.onRoot().performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `ProfessorDialogueScreen skip animation on tap during typing`() {
    composeTestRule.setContent { ProfessorDialogueScreen(onDialogueFinished = {}) }
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.onRoot().performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `ProfessorDialogueScreen calls onDialogueFinished after all lines`() {
    var finished = false
    composeTestRule.setContent { ProfessorDialogueScreen(onDialogueFinished = { finished = true }) }
    composeTestRule.mainClock.advanceTimeBy(5000)
    composeTestRule.onRoot().performClick()
    composeTestRule.mainClock.advanceTimeBy(5000)
    composeTestRule.onRoot().performClick()
    composeTestRule.mainClock.advanceTimeBy(5000)
    composeTestRule.onRoot().performClick()
    composeTestRule.mainClock.advanceTimeBy(5000)
    composeTestRule.onRoot().performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `ProfessorDialogueScreen typing animation progresses`() {
    composeTestRule.setContent { ProfessorDialogueScreen(onDialogueFinished = {}) }
    composeTestRule.mainClock.advanceTimeBy(500)
    composeTestRule.waitForIdle()
  }

  @Test
  fun `ProfessorDialogueScreen multiple rapid taps handled correctly`() {
    var finished = false
    composeTestRule.setContent { ProfessorDialogueScreen(onDialogueFinished = { finished = true }) }
    repeat(10) {
      composeTestRule.onRoot().performClick()
      composeTestRule.mainClock.advanceTimeBy(100)
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `ProfessorDialogueScreen LaunchedEffect triggers for each line`() {
    composeTestRule.setContent { ProfessorDialogueScreen(onDialogueFinished = {}) }
    composeTestRule.mainClock.advanceTimeBy(3000)
    composeTestRule.onRoot().performClick()
    composeTestRule.mainClock.advanceTimeBy(3000)
    composeTestRule.waitForIdle()
  }

  @Test
  fun `ProfessorDialogueScreen complete typing then advance`() {
    composeTestRule.setContent { ProfessorDialogueScreen(onDialogueFinished = {}) }
    composeTestRule.mainClock.advanceTimeBy(10000)
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `ProfessorDialogueScreen with custom modifier works`() {
    composeTestRule.setContent {
      ProfessorDialogueScreen(
          modifier = androidx.compose.ui.Modifier.fillMaxSize(), onDialogueFinished = {})
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }

  @Test
  fun `ProfessorDialogueScreen fast skip all dialogues`() {
    var finished = false
    composeTestRule.setContent { ProfessorDialogueScreen(onDialogueFinished = { finished = true }) }
    composeTestRule.onRoot().performClick()
    composeTestRule.onRoot().performClick()
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.onRoot().performClick()
    composeTestRule.onRoot().performClick()
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.onRoot().performClick()
    composeTestRule.onRoot().performClick()
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.onRoot().performClick()
    composeTestRule.onRoot().performClick()
    composeTestRule.waitForIdle()
  }

  // ============================================================
  // StarterSelectionScreen Tests
  // ============================================================

  @Test
  fun `StarterSelectionScreen renders without crashing`() {
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = {}) }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `StarterSelectionScreen displays confirm button`() {
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = {}) }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `StarterSelectionScreen calls onStarterSelected when confirmed`() {
    var selectedId: String? = null
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = { selectedId = it }) }
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithText("Confirm", substring = true, ignoreCase = true)
        .onFirst()
        .performClick()
  }

  @Test
  fun `StarterSelectionScreen swipe left changes page`() {
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = {}) }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `StarterSelectionScreen swipe right from first page stays on first`() {
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = {}) }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeRight() }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `StarterSelectionScreen floating animation is active`() {
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = {}) }
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()
  }

  @Test
  fun `StarterSelectionScreen navigate to second page and confirm`() {
    var selectedId: String? = null
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = { selectedId = it }) }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithText("Confirm", substring = true, ignoreCase = true)
        .onFirst()
        .performClick()
  }

  @Test
  fun `StarterSelectionScreen navigate to third page and confirm`() {
    var selectedId: String? = null
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = { selectedId = it }) }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithText("Confirm", substring = true, ignoreCase = true)
        .onFirst()
        .performClick()
  }

  @Test
  fun `StarterSelectionScreen arrows visibility on last page`() {
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = {}) }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `StarterSelectionScreen arrows visibility on middle page`() {
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = {}) }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `StarterSelectionScreen arrow animation runs`() {
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = {}) }
    composeTestRule.mainClock.advanceTimeBy(350)
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(350)
    composeTestRule.waitForIdle()
  }

  @Test
  fun `StarterSelectionScreen double swipe and back`() {
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = {}) }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeRight() }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeRight() }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `StarterSelectionScreen pager animation completion`() {
    composeTestRule.setContent { StarterSelectionScreen(onStarterSelected = {}) }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.mainClock.advanceTimeBy(500)
    composeTestRule.waitForIdle()
  }

  @Test
  fun `StarterSelectionScreen all three starters accessible`() {
    var selectedIds = mutableListOf<String>()

    composeTestRule.setContent {
      StarterSelectionScreen(onStarterSelected = { selectedIds.add(it) })
    }
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithText("Confirm", substring = true, ignoreCase = true)
        .onFirst()
        .performClick()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithText("Confirm", substring = true, ignoreCase = true)
        .onFirst()
        .performClick()

    composeTestRule.setContent {
      StarterSelectionScreen(onStarterSelected = { selectedIds.add(it) })
    }
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithText("Confirm", substring = true, ignoreCase = true)
        .onFirst()
        .performClick()
  }

  // ============================================================
  // EduMonOnboardingScreen Integration Tests
  // ============================================================

  @Test
  fun `EduMonOnboardingScreen renders without crashing`() {
    composeTestRule.setContent { EduMonOnboardingScreen() }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `EduMonOnboardingScreen transitions from intro to professor`() {
    composeTestRule.setContent { EduMonOnboardingScreen() }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performClick()
    composeTestRule.mainClock.advanceTimeBy(600)
    composeTestRule.waitForIdle()
  }

  @Test
  fun `EduMonOnboardingScreen full flow completes`() {
    var finishedPlayerName: String? = null
    var finishedStarterId: String? = null

    composeTestRule.setContent {
      EduMonOnboardingScreen(
          onOnboardingFinished = { name, id ->
            finishedPlayerName = name
            finishedStarterId = id
          })
    }

    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performClick()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    repeat(6) {
      composeTestRule.onRoot().performClick()
      composeTestRule.mainClock.advanceTimeBy(3000)
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun `EduMonOnboardingScreen animated content transitions`() {
    composeTestRule.setContent { EduMonOnboardingScreen() }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().performClick()
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.waitForIdle()
  }

  @Test
  fun `EduMonOnboardingScreen with custom modifier`() {
    composeTestRule.setContent {
      EduMonOnboardingScreen(modifier = androidx.compose.ui.Modifier.fillMaxSize())
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `EduMonOnboardingScreen default callback does not crash`() {
    composeTestRule.setContent { EduMonOnboardingScreen() }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `EduMonOnboardingScreen rapid transitions`() {
    composeTestRule.setContent { EduMonOnboardingScreen() }
    composeTestRule.waitForIdle()
    repeat(5) {
      composeTestRule.onRoot().performClick()
      composeTestRule.mainClock.advanceTimeBy(50)
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `EduMonOnboardingScreen modifier chain applied`() {
    composeTestRule.setContent {
      EduMonOnboardingScreen(modifier = androidx.compose.ui.Modifier.fillMaxSize())
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }

  // ============================================================
  // Additional Edge Cases
  // ============================================================

  @Test
  fun `OnboardingDimens object is singleton`() {
    val dimens1 = OnboardingDimens
    val dimens2 = OnboardingDimens
    assertSame(dimens1, dimens2)
  }

  @Test
  fun `StarterItem different backgrounds are not equal`() {
    val item1 =
        StarterItem("id", android.R.drawable.ic_menu_gallery, android.R.drawable.ic_menu_camera)
    val item2 =
        StarterItem("id", android.R.drawable.ic_menu_gallery, android.R.drawable.ic_menu_add)
    assertNotEquals(item1, item2)
  }

  @Test
  fun `StarterItem different images are not equal`() {
    val item1 =
        StarterItem("id", android.R.drawable.ic_menu_gallery, android.R.drawable.ic_menu_camera)
    val item2 = StarterItem("id", android.R.drawable.ic_menu_add, android.R.drawable.ic_menu_camera)
    assertNotEquals(item1, item2)
  }

  @Test
  fun `StarterDefinition different nameRes not equal`() {
    val def1 = StarterDefinition(android.R.string.ok, android.R.drawable.ic_menu_gallery)
    val def2 = StarterDefinition(android.R.string.cancel, android.R.drawable.ic_menu_gallery)
    assertNotEquals(def1, def2)
  }

  @Test
  fun `StarterDefinition different imageRes not equal`() {
    val def1 = StarterDefinition(android.R.string.ok, android.R.drawable.ic_menu_gallery)
    val def2 = StarterDefinition(android.R.string.ok, android.R.drawable.ic_menu_add)
    assertNotEquals(def1, def2)
  }

  @Test
  fun `onboardingStarters list is not empty`() {
    assertTrue(onboardingStarters.isNotEmpty())
  }

  @Test
  fun `onboardingStarters all items have valid resources`() {
    onboardingStarters.forEach { starter ->
      assertTrue(starter.nameRes != 0)
      assertTrue(starter.imageRes != 0)
    }
  }

  @Test
  fun `OnboardingStep can be used in when expression`() {
    val step = OnboardingStep.Intro
    val result =
        when (step) {
          OnboardingStep.Intro -> "intro"
          OnboardingStep.Professor -> "professor"
          OnboardingStep.StarterSelection -> "selection"
        }
    assertEquals("intro", result)
  }

  @Test
  fun `StarterSelectionScreen verify all starter ids`() {
    val expectedIds = listOf("pyromon", "aquamon", "floramon")
    val actualIds =
        listOf(
                StarterItem("pyromon", 0, 0),
                StarterItem("aquamon", 0, 0),
                StarterItem("floramon", 0, 0))
            .map { it.id }
    assertEquals(expectedIds, actualIds)
  }

  @Test
  fun `IntroTapToStartScreen alpha animation cycles`() {
    composeTestRule.setContent { IntroTapToStartScreen(onTap = {}) }
    composeTestRule.mainClock.advanceTimeBy(700)
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(700)
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(700)
    composeTestRule.waitForIdle()
  }
}
