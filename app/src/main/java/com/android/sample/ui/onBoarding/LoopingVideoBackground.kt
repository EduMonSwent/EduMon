package com.android.sample.ui.onBoarding

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@UnstableApi
@Composable
fun LoopingVideoBackgroundFromAssets(assetFileName: String, modifier: Modifier = Modifier) {
  val context = LocalContext.current

  val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
      val mediaItem = MediaItem.fromUri("asset:///$assetFileName")
      setMediaItem(mediaItem)
      repeatMode = Player.REPEAT_MODE_ALL
      playWhenReady = true
      volume = 0f
      prepare()
    }
  }

  DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

  AndroidView(
      modifier = modifier,
      factory = { ctx ->
        PlayerView(ctx).apply {
          player = exoPlayer
          useController = false
          resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
          layoutParams =
              ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
      })
}
