package com.android.sample.ui.onBoarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun EduMonOnboardingScreen(onOnboardingFinished: (userName: String, starterId: String) -> Unit) {
  var currentStep by remember { mutableStateOf(OnboardingStep.Professor) }
  var userName by remember { mutableStateOf("") }

  Box(modifier = Modifier.fillMaxSize()) {
    LoopingVideoBackgroundFromAssets(
        assetFileName = "onboarding_background_epfl.mp4", modifier = Modifier.fillMaxSize())

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
          (fadeIn(animationSpec = tween(OnboardingDimens.transitionDurationMillis)) +
              slideInHorizontally(
                  animationSpec = tween(OnboardingDimens.transitionDurationMillis),
                  initialOffsetX = { fullWidth ->
                    fullWidth / OnboardingDimens.transitionSlideOffsetDivisor
                  })) togetherWith
              (fadeOut(animationSpec = tween(OnboardingDimens.transitionDurationMillis)) +
                  slideOutHorizontally(
                      animationSpec = tween(OnboardingDimens.transitionDurationMillis),
                      targetOffsetX = { fullWidth ->
                        -fullWidth / OnboardingDimens.transitionSlideOffsetDivisor
                      }))
        },
        label = "onboarding_step") { step ->
          when (step) {
            OnboardingStep.Intro -> {
              currentStep = OnboardingStep.Professor
            }
            OnboardingStep.Professor -> {
              ProfessorDialogueScreen(
                  onDialogueFinished = { currentStep = OnboardingStep.StarterSelection })
            }
            OnboardingStep.StarterSelection -> {
              StarterSelectionScreen(
                  onStarterSelected = { starterId -> onOnboardingFinished(userName, starterId) })
            }
          }
        }
  }
}
