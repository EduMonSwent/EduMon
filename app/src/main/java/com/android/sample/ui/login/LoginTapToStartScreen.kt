package com.android.sample.ui.login

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.android.sample.ui.onBoarding.IntroTapToStartScreen
import com.android.sample.ui.onBoarding.LoopingVideoBackgroundFromAssets

@OptIn(UnstableApi::class)
@Composable
fun LoginTapToStartScreen(onTap: () -> Unit) {
  Box(modifier = Modifier.fillMaxSize()) {
    LoopingVideoBackgroundFromAssets(
        assetFileName = "onboarding_background_epfl.mp4", modifier = Modifier.fillMaxSize())
    IntroTapToStartScreen(onTap = onTap)
  }
}
