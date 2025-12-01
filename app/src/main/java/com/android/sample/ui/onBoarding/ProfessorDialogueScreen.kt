package com.android.sample.ui.onBoarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.android.sample.R
import kotlinx.coroutines.delay

// This code has been written partially using A.I (LLM).
@Composable
fun ProfessorDialogueScreen(modifier: Modifier = Modifier, onDialogueFinished: () -> Unit) {
  val lines =
      listOf(
          stringResource(R.string.onboarding_prof_line_1),
          stringResource(R.string.onboarding_prof_line_2),
          stringResource(R.string.onboarding_prof_line_3))

  val professorImages =
      listOf(
          R.drawable.onboarding_prof_line1,
          R.drawable.onboarding_prof_line2,
          R.drawable.onboarding_prof_line3)

  var currentLineIndex by remember { mutableStateOf(0) }
  var visibleCharCount by remember { mutableStateOf(0) }

  val currentLine = lines[currentLineIndex]

  LaunchedEffect(currentLineIndex) {
    visibleCharCount = 0
    currentLine.forEachIndexed { index, _ ->
      delay(OnboardingDimens.dialogueLetterDelayMillis)
      visibleCharCount = index + 1
    }
  }

  Box(
      modifier =
          modifier.fillMaxSize().padding(OnboardingDimens.screenPadding).clickable(
              interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (visibleCharCount < currentLine.length) {
                  visibleCharCount = currentLine.length
                } else {
                  if (currentLineIndex < lines.lastIndex) {
                    currentLineIndex++
                  } else {
                    onDialogueFinished()
                  }
                }
              }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally) {
              AnimatedVisibility(
                  visible = true,
                  enter =
                      fadeIn(animationSpec = tween(OnboardingDimens.transitionDurationMillis)) +
                          slideInVertically(
                              animationSpec = tween(OnboardingDimens.transitionDurationMillis)) {
                                  fullHeight ->
                                fullHeight / 10
                              }) {
                    Image(
                        painter = painterResource(professorImages[currentLineIndex]),
                        contentDescription = null,
                        modifier =
                            Modifier.size(OnboardingDimens.professorLargeSize)
                                .offset(y = OnboardingDimens.professorOffsetY))
                  }

              Spacer(modifier = Modifier.height(OnboardingDimens.contentSpacing))

              Surface(
                  modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                  shape = RoundedCornerShape(OnboardingDimens.dialogCornerRadius),
                  color = colorResource(R.color.onboarding_dialog_background),
                  border =
                      BorderStroke(
                          width = OnboardingDimens.dialogBorderWidth,
                          color = colorResource(R.color.onboarding_dialog_border))) {
                    Box(modifier = Modifier.padding(OnboardingDimens.dialogPadding)) {
                      Text(
                          text = currentLine.take(visibleCharCount),
                          color = colorResource(R.color.onboarding_dialog_text),
                          fontSize = OnboardingDimens.bodyTextSize,
                          textAlign = TextAlign.Start,
                          modifier = Modifier.fillMaxWidth(),
                          maxLines = OnboardingDimens.dialogMaxLines)
                    }
                  }
            }
      }
}
