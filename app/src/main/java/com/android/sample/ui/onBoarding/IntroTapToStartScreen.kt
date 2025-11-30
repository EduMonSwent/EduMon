package com.android.sample.ui.onBoarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.android.sample.R

@Composable
fun IntroTapToStartScreen(
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val tapTextColor = colorResource(R.color.onboarding_tap_to_start_text)

    val infiniteTransition = rememberInfiniteTransition(label = "tap_to_start_blink")
    val tapAlpha by infiniteTransition.animateFloat(
        initialValue = OnboardingDimens.tapBlinkAlphaMin,
        targetValue = OnboardingDimens.tapBlinkAlphaMax,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = OnboardingDimens.tapBlinkDurationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tap_to_start_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(OnboardingDimens.screenPadding)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
    ) {
        Text(
            text = stringResource(R.string.tap_to_start),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = OnboardingDimens.tapToStartBottomPadding),
            color = tapTextColor.copy(alpha = tapAlpha),
            fontSize = OnboardingDimens.bodyTextSize,
            textAlign = TextAlign.Center
        )
    }
}
