// EduMonOnboardingScreen.kt
package com.android.sample.ui.onBoarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import com.android.sample.R

// This code has been written partially using A.I (LLM).
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalAnimationApi::class, UnstableApi::class)
@Composable
fun EduMonOnboardingScreen(
    modifier: Modifier = Modifier,
    onOnboardingFinished: (playerName: String, starterId: String) -> Unit = { _, _ -> }
) {
  var currentStep by remember { mutableStateOf(OnboardingStep.Intro) }

  Box(modifier = modifier.fillMaxSize().background(colorResource(R.color.onboarding_background))) {
    LoopingVideoBackgroundFromAssets(
        assetFileName = "onboarding_background_epfl.mp4", modifier = Modifier.fillMaxSize())

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
          val duration = OnboardingDimens.transitionDurationMillis
          (fadeIn(animationSpec = tween(duration)) +
                  slideInHorizontally(animationSpec = tween(duration)) { fullWidth ->
                    fullWidth / OnboardingDimens.transitionSlideOffsetDivisor
                  })
              .togetherWith(
                  fadeOut(animationSpec = tween(duration)) +
                      slideOutHorizontally(animationSpec = tween(duration)) { fullWidth ->
                        -fullWidth / OnboardingDimens.transitionSlideOffsetDivisor
                      })
        },
        modifier = Modifier.fillMaxSize(),
        label = "edumon_onboarding_step") { step ->
          when (step) {
            OnboardingStep.Intro ->
                IntroTapToStartScreen(onTap = { currentStep = OnboardingStep.Professor })
            OnboardingStep.Professor ->
                ProfessorDialogueScreen(
                    onDialogueFinished = { currentStep = OnboardingStep.StarterSelection })
            OnboardingStep.StarterSelection -> {
              val defaultPlayerName = stringResource(R.string.onboarding_default_player_name)

              StarterSelectionScreen(
                  onStarterSelected = { starterId ->
                    onOnboardingFinished(defaultPlayerName, starterId)
                  })
            }
          }
        }
  }
}
